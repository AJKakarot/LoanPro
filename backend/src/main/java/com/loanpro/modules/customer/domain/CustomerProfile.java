package com.loanpro.modules.customer.domain;

import com.loanpro.common.domain.BaseEntity;
import com.loanpro.modules.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 40)
    private String nationalId;

    private String addressLine;
    private String city;
    private String state;
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmploymentType employmentType;

    private String employerName;
    private String designation;
    private Integer yearsEmployed;

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(precision = 15, scale = 2)
    private BigDecimal otherIncome;

    @Column(precision = 15, scale = 2)
    private BigDecimal existingEmis;

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyExpenses;
}
