package com.loanpro.modules.dashboard.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.modules.dashboard.dto.DashboardStatsResponse;
import com.loanpro.modules.dashboard.service.DashboardService;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<DashboardStatsResponse> stats(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(dashboardService.stats(currentUserService.require(principal)));
    }
}
