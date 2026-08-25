package com.loanpro.modules.audit.domain;

import com.loanpro.common.domain.BaseEntity;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 180)
    private String userEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String entityType;

    private java.util.UUID entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ApplicationStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 64)
    private String ipAddress;
}
