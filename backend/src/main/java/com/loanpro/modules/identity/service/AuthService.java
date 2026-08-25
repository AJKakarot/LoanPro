package com.loanpro.modules.identity.service;

import com.loanpro.common.exception.BusinessException;
import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.customer.domain.CustomerProfile;
import com.loanpro.modules.customer.repository.CustomerProfileRepository;
import com.loanpro.modules.identity.domain.RefreshToken;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.domain.UserStatus;
import com.loanpro.modules.identity.dto.AuthResponse;
import com.loanpro.modules.identity.dto.ChangePasswordRequest;
import com.loanpro.modules.identity.dto.LoginRequest;
import com.loanpro.modules.identity.dto.RefreshRequest;
import com.loanpro.modules.identity.dto.RegisterRequest;
import com.loanpro.modules.identity.dto.UpdateAccountRequest;
import com.loanpro.modules.identity.dto.UserResponse;
import com.loanpro.modules.identity.repository.RefreshTokenRepository;
import com.loanpro.modules.identity.repository.RoleRepository;
import com.loanpro.modules.identity.repository.UserRepository;
import com.loanpro.security.JwtService;
import com.loanpro.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final com.loanpro.config.AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CustomerProfileRepository customerProfileRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuditService auditService,
            com.loanpro.config.AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("An account with this email already exists");
        }
        var role = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not seeded"));
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(RoleName.CUSTOMER);
        user.getRoles().add(role);
        userRepository.save(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        customerProfileRepository.save(profile);

        auditService.record(user, "USER_REGISTERED", "User", user.getId(), null, null, null, "Customer self-registration");
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException ex) {
            if (user != null) {
                recordFailedLogin(user);
            }
            throw ex;
        }
        User authenticated = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (authenticated.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }
        authenticated.setFailedLoginAttempts(0);
        authenticated.setLockedUntil(null);
        auditService.record(authenticated, "USER_LOGIN", "User", authenticated.getId(), null, null, null, null);
        return issueTokens(authenticated);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new BusinessException("Refresh token is invalid"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            throw new BusinessException("Refresh token has expired");
        }
        User owner = stored.getUser();
        boolean locked = owner.getLockedUntil() != null && owner.getLockedUntil().isAfter(Instant.now());
        if (owner.getStatus() != UserStatus.ACTIVE || locked) {
            stored.setRevoked(true);
            throw new BusinessException("Account is not active");
        }
        stored.setRevoked(true);
        return issueTokens(owner);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashAndRevokedFalse(sha256(refreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void logoutAll(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        revokeAllRefreshTokens(user);
        user.setTokenVersion(user.getTokenVersion() + 1);
        auditService.record(user, "USER_LOGOUT_ALL", "User", user.getId(), null, null, null, null);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        revokeAllRefreshTokens(user);
        auditService.record(user, "PASSWORD_CHANGED", "User", user.getId(), null, null, null, null);
    }

    @Transactional
    public UserResponse updateAccount(UUID userId, UpdateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        return UserResponse.from(user);
    }

    private void recordFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        int maxAttempts = appProperties.security().lockoutAttempts();
        if (attempts >= maxAttempts) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plus(appProperties.security().lockoutMinutes(), ChronoUnit.MINUTES));
            auditService.record(user, "ACCOUNT_LOCKED", "User", user.getId(), null, null, null, "Too many failed logins");
        }
    }

    private void revokeAllRefreshTokens(User user) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId())
                .forEach(token -> token.setRevoked(true));
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        String access = jwtService.generateAccessToken(principal);
        String refresh = generateRefreshToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(sha256(refresh));
        entity.setExpiresAt(Instant.now().plus(appProperties.jwt().refreshTokenDays(), ChronoUnit.DAYS));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        return AuthResponse.of(access, refresh, toAuthUser(user));
    }

    private AuthResponse.UserResponse toAuthUser(User user) {
        return new AuthResponse.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus().name(),
                user.roleNames()
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
