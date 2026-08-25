package com.loanpro.modules.customer.dto;

import com.loanpro.modules.customer.domain.CustomerProfile;
import com.loanpro.modules.customer.domain.EmploymentType;
import com.loanpro.modules.identity.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String nationalId,
        String addressLine,
        String city,
        String state,
        String postalCode,
        EmploymentType employmentType,
        String employerName,
        String designation,
        Integer yearsEmployed,
        BigDecimal monthlyIncome,
        BigDecimal otherIncome,
        BigDecimal existingEmis,
        BigDecimal monthlyExpenses
) {
    public static CustomerProfileResponse from(User user, CustomerProfile profile) {
        return new CustomerProfileResponse(
                profile.getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getNationalId(),
                profile.getAddressLine(),
                profile.getCity(),
                profile.getState(),
                profile.getPostalCode(),
                profile.getEmploymentType(),
                profile.getEmployerName(),
                profile.getDesignation(),
                profile.getYearsEmployed(),
                profile.getMonthlyIncome(),
                profile.getOtherIncome(),
                profile.getExistingEmis(),
                profile.getMonthlyExpenses()
        );
    }
}
