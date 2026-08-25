package com.loanpro.modules.application.repository;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    @EntityGraph(attributePaths = {"customer", "loanProduct", "assignedMaker", "assignedChecker"})
    @Query("SELECT a FROM LoanApplication a WHERE a.id = :id")
    Optional<LoanApplication> findWithDetailsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"customer", "loanProduct"})
    @Query("""
            SELECT a FROM LoanApplication a
            WHERE a.customer.id = :customerId
              AND (:status IS NULL OR a.status = :status)
              AND (:search = '' OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoanApplication> findCustomerApplications(
            @Param("customerId") UUID customerId,
            @Param("status") ApplicationStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "loanProduct", "assignedMaker", "assignedChecker"})
    @Query("""
            SELECT a FROM LoanApplication a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:search = '' OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.customer.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoanApplication> searchAll(
            @Param("status") ApplicationStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "loanProduct", "assignedMaker"})
    @Query("""
            SELECT a FROM LoanApplication a
            WHERE a.status IN :statuses
              AND (:search = '' OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoanApplication> findByStatusIn(
            @Param("statuses") Collection<ApplicationStatus> statuses,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(ApplicationStatus status);

    long countByCustomer_IdAndIdNot(UUID customerId, UUID id);

    @Query("SELECT COALESCE(SUM(a.requestedAmount), 0) FROM LoanApplication a WHERE a.status = :status")
    BigDecimal sumRequestedAmountByStatus(@Param("status") ApplicationStatus status);

    @Query("SELECT COALESCE(SUM(a.requestedAmount), 0) FROM LoanApplication a")
    BigDecimal sumRequestedAmount();
}
