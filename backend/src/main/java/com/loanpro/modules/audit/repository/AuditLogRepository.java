package com.loanpro.modules.audit.repository;

import com.loanpro.modules.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:search = '' OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.userEmail, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.remarks, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:applicationId IS NULL OR a.application.id = :applicationId)
            """)
    Page<AuditLog> search(
            @Param("search") String search,
            @Param("applicationId") UUID applicationId,
            Pageable pageable
    );
}
