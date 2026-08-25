package com.loanpro.modules.application.repository;

import com.loanpro.modules.application.domain.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {
    @Query("""
            select h from ApplicationStatusHistory h
            join fetch h.changedBy
            where h.application.id = :applicationId
            order by h.createdAt asc
            """)
    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAsc(@Param("applicationId") UUID applicationId);
}
