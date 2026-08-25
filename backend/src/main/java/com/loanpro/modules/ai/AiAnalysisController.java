package com.loanpro.modules.ai;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.modules.ai.dto.AiAnalysisView;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;
    private final CurrentUserService currentUserService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService, CurrentUserService currentUserService) {
        this.aiAnalysisService = aiAnalysisService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{id}/ai-analysis")
    @PreAuthorize("hasAnyRole('MAKER','CHECKER','ADMIN')")
    public ApiResponse<AiAnalysisView> analyze(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(aiAnalysisService.analyze(id, currentUserService.require(principal)));
    }
}
