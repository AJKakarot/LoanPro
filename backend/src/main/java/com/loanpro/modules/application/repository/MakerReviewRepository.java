package com.loanpro.modules.application.repository;

import com.loanpro.modules.application.domain.MakerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MakerReviewRepository extends JpaRepository<MakerReview, UUID> {
    List<MakerReview> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
    Optional<MakerReview> findFirstByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
