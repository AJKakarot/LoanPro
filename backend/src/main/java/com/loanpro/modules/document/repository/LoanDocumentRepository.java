package com.loanpro.modules.document.repository;

import com.loanpro.modules.document.domain.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, UUID> {
    List<LoanDocument> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
    long countByApplicationId(UUID applicationId);
}
