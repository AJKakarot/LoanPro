package com.loanpro.modules.workflow.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.common.api.PageResponse;
import com.loanpro.modules.application.dto.ApplicationDetailResponse;
import com.loanpro.modules.application.dto.ApplicationSummaryResponse;
import com.loanpro.modules.application.dto.CheckerReviewResponse;
import com.loanpro.modules.application.dto.DecisionRequest;
import com.loanpro.modules.application.dto.RemarksRequest;
import com.loanpro.modules.workflow.dto.EligibilityResponse;
import com.loanpro.modules.workflow.service.WorkflowService;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checker")
@PreAuthorize("hasRole('CHECKER')")
public class CheckerController {

    private final WorkflowService workflowService;
    private final CurrentUserService currentUserService;

    public CheckerController(WorkflowService workflowService, CurrentUserService currentUserService) {
        this.workflowService = workflowService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponse<ApplicationSummaryResponse>> queue(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.ok(workflowService.checkerQueue(search, pageable));
    }

    @GetMapping("/applications/{id}/eligibility")
    public ApiResponse<EligibilityResponse> eligibility(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.eligibility(id, currentUserService.require(principal)));
    }

    @PostMapping("/applications/{id}/approve")
    public ApiResponse<ApplicationDetailResponse> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) DecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String remarks = request == null ? "Approved" : request.remarks();
        return ApiResponse.ok("Application approved",
                workflowService.approve(id, remarks, currentUserService.require(principal)));
    }

    @PostMapping("/applications/{id}/reject")
    public ApiResponse<ApplicationDetailResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Application rejected",
                workflowService.reject(id, request.reason(), request.remarks(), currentUserService.require(principal)));
    }

    @PostMapping("/applications/{id}/return")
    public ApiResponse<ApplicationDetailResponse> returnToMaker(
            @PathVariable UUID id,
            @Valid @RequestBody RemarksRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.returnToMaker(id, request.remarks(), currentUserService.require(principal)));
    }

    @GetMapping("/applications/{id}/reviews")
    public ApiResponse<List<CheckerReviewResponse>> reviews(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.checkerHistory(id, currentUserService.require(principal)));
    }
}
