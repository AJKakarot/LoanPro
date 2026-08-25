package com.loanpro.modules.application.repository;

import com.loanpro.modules.application.domain.CheckerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckerReviewRepository extends JpaRepository<CheckerReview, UUID> {
    List<CheckerReview> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
    Optional<CheckerReview> findFirstByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
