package com.loanpro.modules.identity.service;

import com.loanpro.common.api.PageResponse;
import com.loanpro.common.exception.BusinessException;
import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.customer.domain.CustomerProfile;
import com.loanpro.modules.customer.repository.CustomerProfileRepository;
import com.loanpro.modules.identity.domain.Role;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.domain.UserStatus;
import com.loanpro.modules.identity.dto.CreateUserRequest;
import com.loanpro.modules.identity.dto.RoleResponse;
import com.loanpro.modules.identity.dto.UpdateUserRequest;
import com.loanpro.modules.identity.dto.UserResponse;
import com.loanpro.modules.identity.repository.RoleRepository;
import com.loanpro.modules.identity.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String search, UserStatus status, RoleName role, Pageable pageable) {
        String q = search == null || search.isBlank() ? "" : search.trim();
        return PageResponse.from(userRepository.search(q, status, role, pageable).map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, User actor) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        user.setRoles(resolveRoles(request.roles()));
        user.setRole(primaryRole(request.roles()));
        userRepository.save(user);
        if (user.hasRole(RoleName.CUSTOMER)) {
            CustomerProfile profile = new CustomerProfile();
            profile.setUser(user);
            customerProfileRepository.save(profile);
        }
        auditService.record(actor, "USER_CREATED", "User", user.getId(), null, null, null, user.getEmail());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, User actor) {
        User user = requireUser(id);
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(resolveRoles(request.roles()));
            user.setRole(primaryRole(request.roles()));
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        auditService.record(actor, "USER_UPDATED", "User", user.getId(), null, null, null, user.getEmail());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    private RoleName primaryRole(Set<RoleName> names) {
        if (names.contains(RoleName.ADMIN)) {
            return RoleName.ADMIN;
        }
        if (names.contains(RoleName.CHECKER)) {
            return RoleName.CHECKER;
        }
        if (names.contains(RoleName.MAKER)) {
            return RoleName.MAKER;
        }
        return RoleName.CUSTOMER;
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name)));
        }
        return roles;
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
