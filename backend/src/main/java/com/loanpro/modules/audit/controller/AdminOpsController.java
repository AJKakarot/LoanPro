package com.loanpro.modules.audit.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.common.api.PageResponse;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.dto.ApplicationSummaryResponse;
import com.loanpro.modules.audit.dto.AuditLogResponse;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.workflow.service.WorkflowService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOpsController {

    private final AuditService auditService;
    private final WorkflowService workflowService;

    public AdminOpsController(AuditService auditService, WorkflowService workflowService) {
        this.auditService = auditService;
        this.workflowService = workflowService;
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> auditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID applicationId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.ok(auditService.search(search, applicationId, pageable));
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponse<ApplicationSummaryResponse>> applications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.ok(workflowService.adminSearch(status, search, pageable));
    }
}
