package com.loanpro.modules.application.domain;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    MAKER_REVIEW,
    INFO_REQUESTED,
    MAKER_VERIFIED,
    CHECKER_REVIEW,
    RETURNED_TO_MAKER,
    APPROVED,
    REJECTED
}
