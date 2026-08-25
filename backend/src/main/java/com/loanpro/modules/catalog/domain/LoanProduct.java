package com.loanpro.modules.catalog.domain;

import com.loanpro.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "loan_products")
public class LoanProduct extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private Integer minTenureMonths;

    @Column(nullable = false)
    private Integer maxTenureMonths;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal interestRate;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal processingFeePercent;

    @Column(nullable = false)
    private String requiredDocuments;

    @Column(nullable = false)
    private boolean active = true;
}
