package com.loanpro.modules.customer.service;

import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.customer.domain.CustomerProfile;
import com.loanpro.modules.customer.dto.CustomerProfileResponse;
import com.loanpro.modules.customer.dto.UpdateProfileRequest;
import com.loanpro.modules.customer.repository.CustomerProfileRepository;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerProfileService {

    private final CustomerProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CustomerProfileService(
            CustomerProfileRepository profileRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse get(UUID userId) {
        User user = requireUser(userId);
        return CustomerProfileResponse.from(user, requireProfile(userId));
    }

    @Transactional
    public CustomerProfileResponse update(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        CustomerProfile profile = requireProfile(userId);
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        if (request.nationalId() != null) {
            profile.setNationalId(request.nationalId());
        }
        if (request.addressLine() != null) {
            profile.setAddressLine(request.addressLine());
        }
        if (request.city() != null) {
            profile.setCity(request.city());
        }
        if (request.state() != null) {
            profile.setState(request.state());
        }
        if (request.postalCode() != null) {
            profile.setPostalCode(request.postalCode());
        }
        if (request.employmentType() != null) {
            profile.setEmploymentType(request.employmentType());
        }
        if (request.employerName() != null) {
            profile.setEmployerName(request.employerName());
        }
        if (request.designation() != null) {
            profile.setDesignation(request.designation());
        }
        if (request.yearsEmployed() != null) {
            profile.setYearsEmployed(request.yearsEmployed());
        }
        if (request.monthlyIncome() != null) {
            profile.setMonthlyIncome(request.monthlyIncome());
        }
        if (request.otherIncome() != null) {
            profile.setOtherIncome(request.otherIncome());
        }
        if (request.existingEmis() != null) {
            profile.setExistingEmis(request.existingEmis());
        }
        if (request.monthlyExpenses() != null) {
            profile.setMonthlyExpenses(request.monthlyExpenses());
        }
        auditService.record(user, "PROFILE_UPDATED", "CustomerProfile", profile.getId(), null, null, null, null);
        return CustomerProfileResponse.from(user, profile);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CustomerProfile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
    }
}
