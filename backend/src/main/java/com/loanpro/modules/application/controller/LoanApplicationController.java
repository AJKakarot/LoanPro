package com.loanpro.modules.application.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.common.api.PageResponse;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.dto.ApplicationDetailResponse;
import com.loanpro.modules.application.dto.ApplicationSummaryResponse;
import com.loanpro.modules.application.dto.StatusHistoryResponse;
import com.loanpro.modules.application.dto.UpsertApplicationRequest;
import com.loanpro.modules.application.repository.ApplicationStatusHistoryRepository;
import com.loanpro.modules.application.service.LoanApplicationService;
import com.loanpro.modules.workflow.service.WorkflowService;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class LoanApplicationController {

    private final LoanApplicationService applicationService;
    private final WorkflowService workflowService;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final CurrentUserService currentUserService;

    public LoanApplicationController(
            LoanApplicationService applicationService,
            WorkflowService workflowService,
            ApplicationStatusHistoryRepository historyRepository,
            CurrentUserService currentUserService
    ) {
        this.applicationService = applicationService;
        this.workflowService = workflowService;
        this.historyRepository = historyRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationDetailResponse> create(
            @Valid @RequestBody UpsertApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Application created",
                ApplicationDetailResponse.from(applicationService.create(principal.getId(), request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PageResponse<ApplicationSummaryResponse>> mine(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.customerList(principal.getId(), status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ApiResponse<ApplicationDetailResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var actor = currentUserService.require(principal);
        return ApiResponse.ok(ApplicationDetailResponse.from(applicationService.requireVisibleTo(id, actor)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ApplicationDetailResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(ApplicationDetailResponse.from(applicationService.update(id, principal.getId(), request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ApplicationDetailResponse> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Application submitted",
                ApplicationDetailResponse.from(applicationService.submit(id, principal.getId())));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ApiResponse<List<StatusHistoryResponse>> history(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        applicationService.requireVisibleTo(id, currentUserService.require(principal));
        return ApiResponse.ok(historyRepository.findByApplicationIdOrderByCreatedAtAsc(id)
                .stream().map(StatusHistoryResponse::from).toList());
    }

    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ApiResponse<com.loanpro.modules.workflow.dto.EligibilityResponse> eligibility(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.eligibility(id, currentUserService.require(principal)));
    }

    @GetMapping("/{id}/maker-reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ApiResponse<List<com.loanpro.modules.application.dto.MakerReviewResponse>> makerReviews(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.makerHistory(id, currentUserService.require(principal)));
    }

    @GetMapping("/{id}/checker-reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ApiResponse<List<com.loanpro.modules.application.dto.CheckerReviewResponse>> checkerReviews(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.checkerHistory(id, currentUserService.require(principal)));
    }
}
