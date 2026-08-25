package com.loanpro.modules.catalog.service;

import com.loanpro.common.exception.BusinessException;
import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.catalog.domain.LoanProduct;
import com.loanpro.modules.catalog.dto.LoanProductRequest;
import com.loanpro.modules.catalog.dto.LoanProductResponse;
import com.loanpro.modules.catalog.repository.LoanProductRepository;
import com.loanpro.modules.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;
    private final AuditService auditService;

    public LoanProductService(LoanProductRepository loanProductRepository, AuditService auditService) {
        this.loanProductRepository = loanProductRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LoanProductResponse> list(boolean activeOnly) {
        var products = activeOnly
                ? loanProductRepository.findByActiveTrueOrderByNameAsc()
                : loanProductRepository.findAll();
        return products.stream().map(LoanProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LoanProductResponse get(UUID id) {
        return LoanProductResponse.from(require(id));
    }

    @Transactional
    public LoanProductResponse create(LoanProductRequest request, User actor) {
        if (loanProductRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException("Loan product code already exists");
        }
        validateRange(request);
        LoanProduct product = new LoanProduct();
        apply(product, request);
        loanProductRepository.save(product);
        auditService.record(actor, "PRODUCT_CREATED", "LoanProduct", product.getId(), null, null, null, product.getCode());
        return LoanProductResponse.from(product);
    }

    @Transactional
    public LoanProductResponse update(UUID id, LoanProductRequest request, User actor) {
        LoanProduct product = require(id);
        loanProductRepository.findByCodeIgnoreCase(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Loan product code already exists");
                });
        validateRange(request);
        apply(product, request);
        auditService.record(actor, "PRODUCT_UPDATED", "LoanProduct", product.getId(), null, null, null, product.getCode());
        return LoanProductResponse.from(product);
    }

    private void apply(LoanProduct product, LoanProductRequest request) {
        product.setCode(request.code().trim().toUpperCase());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setMinAmount(request.minAmount());
        product.setMaxAmount(request.maxAmount());
        product.setMinTenureMonths(request.minTenureMonths());
        product.setMaxTenureMonths(request.maxTenureMonths());
        product.setInterestRate(request.interestRate());
        product.setProcessingFeePercent(request.processingFeePercent());
        product.setRequiredDocuments(request.requiredDocuments());
        product.setActive(request.active() == null || request.active());
    }

    private void validateRange(LoanProductRequest request) {
        if (request.minAmount().compareTo(request.maxAmount()) > 0) {
            throw new BusinessException("Minimum amount cannot exceed maximum amount");
        }
        if (request.minTenureMonths() > request.maxTenureMonths()) {
            throw new BusinessException("Minimum tenure cannot exceed maximum tenure");
        }
    }

    private LoanProduct require(UUID id) {
        return loanProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));
    }
}
