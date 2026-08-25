package com.loanpro.modules.catalog.repository;

import com.loanpro.modules.catalog.domain.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<LoanProduct> findByCodeIgnoreCase(String code);
    List<LoanProduct> findByActiveTrueOrderByNameAsc();
}
