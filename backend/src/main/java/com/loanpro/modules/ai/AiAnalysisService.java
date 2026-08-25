package com.loanpro.modules.ai;

import com.loanpro.modules.ai.dto.AiAnalysisView;
import com.loanpro.modules.ai.dto.AiAnalyzeResponse;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.application.repository.LoanApplicationRepository;
import com.loanpro.modules.application.service.LoanApplicationService;
import com.loanpro.modules.document.dto.DocumentResponse;
import com.loanpro.modules.document.service.DocumentService;
import com.loanpro.modules.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiAnalysisService {

    private static final String DISCLAIMER =
            "AI-generated analysis. Final decision must be made by an authorized human reviewer.";

    private final LoanApplicationService applicationService;
    private final LoanApplicationRepository applicationRepository;
    private final DocumentService documentService;
    private final AiAnalysisClient aiAnalysisClient;

    public AiAnalysisService(
            LoanApplicationService applicationService,
            LoanApplicationRepository applicationRepository,
            DocumentService documentService,
            AiAnalysisClient aiAnalysisClient
    ) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.documentService = documentService;
        this.aiAnalysisClient = aiAnalysisClient;
    }

    @Transactional(readOnly = true)
    public AiAnalysisView analyze(UUID applicationId, User actor) {
        LoanApplication application = applicationService.requireVisibleTo(applicationId, actor);
        List<DocumentResponse> documents = documentService.list(applicationId, actor);
        long related = applicationRepository.countByCustomer_IdAndIdNot(
                application.getCustomer().getId(), application.getId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("application", applicationPayload(application, related));
        payload.put("documents", documents.stream().map(this::documentPayload).toList());

        return aiAnalysisClient.analyze(payload)
                .map(this::toView)
                .orElseGet(() -> AiAnalysisView.unavailable(
                        "AI analysis is unavailable. Continue the maker-checker review manually."));
    }

    private AiAnalysisView toView(AiAnalyzeResponse response) {
        var summary = response.verificationSummary();
        if (summary == null) {
            return AiAnalysisView.unavailable("AI analysis returned an incomplete result.");
        }
        return new AiAnalysisView(
                true,
                "AI is advisory only. Maker and checker remain responsible for the final decision.",
                summary.documentStatus(),
                summary.eligibilityAssessment(),
                summary.riskLevel(),
                nvl(summary.keyIssues()),
                nvl(summary.recommendedManualChecks()),
                summary.summary(),
                summary.disclaimer() == null || summary.disclaimer().isBlank() ? DISCLAIMER : summary.disclaimer(),
                response
        );
    }

    private Map<String, Object> applicationPayload(LoanApplication application, long related) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", application.getId());
        body.put("applicationNumber", application.getApplicationNumber());
        body.put("status", application.getStatus() == null ? null : application.getStatus().name());
        body.put("fullName", application.getFullName());
        body.put("email", application.getEmail());
        body.put("phone", application.getPhone());
        body.put("nationalId", application.getNationalId());
        body.put("dateOfBirth", application.getDateOfBirth());
        body.put("employmentType", application.getEmploymentType() == null ? null : application.getEmploymentType().name());
        body.put("employerName", application.getEmployerName());
        body.put("designation", application.getDesignation());
        body.put("yearsEmployed", application.getYearsEmployed());
        body.put("monthlyIncome", application.getMonthlyIncome());
        body.put("otherIncome", application.getOtherIncome());
        body.put("existingEmis", application.getExistingEmis());
        body.put("monthlyExpenses", application.getMonthlyExpenses());
        body.put("requestedAmount", application.getRequestedAmount());
        body.put("tenureMonths", application.getTenureMonths());
        body.put("interestRate", application.getInterestRate());
        body.put("purpose", application.getPurpose());
        body.put("loanProductCode", application.getLoanProduct() == null ? null : application.getLoanProduct().getCode());
        body.put("loanProductName", application.getLoanProduct() == null ? null : application.getLoanProduct().getName());
        body.put("requiredDocuments", application.getLoanProduct() == null ? null : application.getLoanProduct().getRequiredDocuments());
        body.put("relatedApplicationCount", related);
        return body;
    }

    private Map<String, Object> documentPayload(DocumentResponse document) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", document.id());
        body.put("documentType", document.documentType() == null ? null : document.documentType().name());
        body.put("originalFileName", document.originalFileName());
        body.put("contentType", document.contentType());
        body.put("fileSize", document.fileSize());
        body.put("verificationStatus", document.verificationStatus() == null ? null : document.verificationStatus().name());
        body.put("verificationRemarks", document.verificationRemarks());
        return body;
    }

    private List<String> nvl(List<String> values) {
        return values == null ? new ArrayList<>() : values;
    }
}
