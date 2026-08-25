package com.loanpro.modules.application.domain;

import com.loanpro.common.domain.BaseEntity;
import com.loanpro.modules.catalog.domain.LoanProduct;
import com.loanpro.modules.customer.domain.EmploymentType;
import com.loanpro.modules.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "loan_applications")
public class LoanApplication extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal interestRate;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal processingFeePercent;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationalId;
    private String phone;
    private String email;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_maker_id")
    private User assignedMaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_checker_id")
    private User assignedChecker;

    private Instant submittedAt;
    private Instant decidedAt;

    @Version
    private Long version;
}
