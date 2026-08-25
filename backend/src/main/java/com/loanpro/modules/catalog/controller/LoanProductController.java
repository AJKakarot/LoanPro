package com.loanpro.modules.catalog.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.modules.catalog.dto.LoanProductRequest;
import com.loanpro.modules.catalog.dto.LoanProductResponse;
import com.loanpro.modules.catalog.service.LoanProductService;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LoanProductController {

    private final LoanProductService loanProductService;
    private final CurrentUserService currentUserService;

    public LoanProductController(LoanProductService loanProductService, CurrentUserService currentUserService) {
        this.loanProductService = loanProductService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/loan-products")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LoanProductResponse>> list() {
        return ApiResponse.ok(loanProductService.list(true));
    }

    @GetMapping("/loan-products/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LoanProductResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(loanProductService.get(id));
    }

    @GetMapping("/admin/loan-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LoanProductResponse>> adminList() {
        return ApiResponse.ok(loanProductService.list(false));
    }

    @PostMapping("/admin/loan-products")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoanProductResponse> create(
            @Valid @RequestBody LoanProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Product created", loanProductService.create(request, currentUserService.require(principal)));
    }

    @PutMapping("/admin/loan-products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LoanProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody LoanProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(loanProductService.update(id, request, currentUserService.require(principal)));
    }
}
