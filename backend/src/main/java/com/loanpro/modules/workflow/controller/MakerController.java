package com.loanpro.modules.workflow.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.common.api.PageResponse;
import com.loanpro.modules.application.dto.ApplicationDetailResponse;
import com.loanpro.modules.application.dto.ApplicationSummaryResponse;
import com.loanpro.modules.application.dto.MakerReviewResponse;
import com.loanpro.modules.application.dto.MakerVerifyRequest;
import com.loanpro.modules.application.dto.RemarksRequest;
import com.loanpro.modules.document.dto.DocumentResponse;
import com.loanpro.modules.document.dto.VerifyDocumentRequest;
import com.loanpro.modules.document.service.DocumentService;
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
@RequestMapping("/api/v1/maker")
@PreAuthorize("hasRole('MAKER')")
public class MakerController {

    private final WorkflowService workflowService;
    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    public MakerController(
            WorkflowService workflowService,
            DocumentService documentService,
            CurrentUserService currentUserService
    ) {
        this.workflowService = workflowService;
        this.documentService = documentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponse<ApplicationSummaryResponse>> queue(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.ok(workflowService.makerQueue(search, pageable));
    }

    @PostMapping("/applications/{id}/claim")
    public ApiResponse<ApplicationDetailResponse> claim(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.claimForMaker(id, currentUserService.require(principal)));
    }

    @PostMapping("/applications/{id}/verify")
    public ApiResponse<MakerReviewResponse> verify(
            @PathVariable UUID id,
            @Valid @RequestBody MakerVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.saveMakerReview(id, request, currentUserService.require(principal)));
    }

    @PostMapping("/applications/{id}/request-info")
    public ApiResponse<ApplicationDetailResponse> requestInfo(
            @PathVariable UUID id,
            @Valid @RequestBody RemarksRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.requestInformation(
                id, request.remarks(), request.missingInformation(), currentUserService.require(principal)));
    }

    @PostMapping("/applications/{id}/send-to-checker")
    public ApiResponse<ApplicationDetailResponse> sendToChecker(
            @PathVariable UUID id,
            @RequestBody(required = false) RemarksRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String remarks = request == null ? "Sent to checker" : request.remarks();
        return ApiResponse.ok(workflowService.sendToChecker(id, remarks, currentUserService.require(principal)));
    }

    @PostMapping("/applications/{applicationId}/documents/{documentId}/verify")
    public ApiResponse<DocumentResponse> verifyDocument(
            @PathVariable UUID applicationId,
            @PathVariable UUID documentId,
            @Valid @RequestBody VerifyDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(documentService.verify(
                documentId, request.verified(), request.remarks(), currentUserService.require(principal)));
    }

    @GetMapping("/applications/{id}/reviews")
    public ApiResponse<List<MakerReviewResponse>> reviews(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(workflowService.makerHistory(id, currentUserService.require(principal)));
    }
}
