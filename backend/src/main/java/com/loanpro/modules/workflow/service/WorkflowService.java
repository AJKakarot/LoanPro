package com.loanpro.modules.workflow.service;

import com.loanpro.common.api.PageResponse;
import com.loanpro.common.exception.BusinessException;
import com.loanpro.common.exception.ForbiddenException;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.CheckerDecision;
import com.loanpro.modules.application.domain.CheckerReview;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.application.domain.MakerReview;
import com.loanpro.modules.application.dto.ApplicationDetailResponse;
import com.loanpro.modules.application.dto.ApplicationSummaryResponse;
import com.loanpro.modules.application.dto.CheckerReviewResponse;
import com.loanpro.modules.application.dto.MakerReviewResponse;
import com.loanpro.modules.application.dto.MakerVerifyRequest;
import com.loanpro.modules.application.repository.CheckerReviewRepository;
import com.loanpro.modules.application.repository.LoanApplicationRepository;
import com.loanpro.modules.application.repository.MakerReviewRepository;
import com.loanpro.modules.application.service.LoanApplicationService;
import com.loanpro.modules.document.domain.DocumentVerificationStatus;
import com.loanpro.modules.document.service.DocumentService;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.notification.service.NotificationService;
import com.loanpro.modules.workflow.dto.EligibilityResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkflowService {

    private static final Set<ApplicationStatus> MAKER_QUEUE = Set.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.MAKER_REVIEW,
            ApplicationStatus.RETURNED_TO_MAKER,
            ApplicationStatus.INFO_REQUESTED
    );
    private static final Set<ApplicationStatus> CHECKER_QUEUE = Set.of(
            ApplicationStatus.MAKER_VERIFIED,
            ApplicationStatus.CHECKER_REVIEW
    );

    private final LoanApplicationRepository applicationRepository;
    private final LoanApplicationService applicationService;
    private final MakerReviewRepository makerReviewRepository;
    private final CheckerReviewRepository checkerReviewRepository;
    private final DocumentService documentService;
    private final EligibilityService eligibilityService;
    private final NotificationService notificationService;

    public WorkflowService(
            LoanApplicationRepository applicationRepository,
            LoanApplicationService applicationService,
            MakerReviewRepository makerReviewRepository,
            CheckerReviewRepository checkerReviewRepository,
            DocumentService documentService,
            EligibilityService eligibilityService,
            NotificationService notificationService
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.makerReviewRepository = makerReviewRepository;
        this.checkerReviewRepository = checkerReviewRepository;
        this.documentService = documentService;
        this.eligibilityService = eligibilityService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> makerQueue(String search, Pageable pageable) {
        return PageResponse.from(
                applicationRepository.findByStatusIn(MAKER_QUEUE, blankToEmpty(search), pageable)
                        .map(ApplicationSummaryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> checkerQueue(String search, Pageable pageable) {
        return PageResponse.from(
                applicationRepository.findByStatusIn(CHECKER_QUEUE, blankToEmpty(search), pageable)
                        .map(ApplicationSummaryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> adminSearch(ApplicationStatus status, String search, Pageable pageable) {
        return PageResponse.from(
                applicationRepository.searchAll(status, blankToEmpty(search), pageable)
                        .map(ApplicationSummaryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> customerList(UUID customerId, ApplicationStatus status, String search, Pageable pageable) {
        return PageResponse.from(
                applicationRepository.findCustomerApplications(customerId, status, blankToEmpty(search), pageable)
                        .map(ApplicationSummaryResponse::from)
        );
    }

    @Transactional
    public ApplicationDetailResponse claimForMaker(UUID applicationId, User maker) {
        LoanApplication application = applicationService.require(applicationId);
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.RETURNED_TO_MAKER) {
            throw new BusinessException("Application is not waiting for maker review");
        }
        application.setAssignedMaker(maker);
        applicationService.transition(application, ApplicationStatus.MAKER_REVIEW, maker, "Maker claimed application");
        return ApplicationDetailResponse.from(application);
    }

    @Transactional
    public MakerReviewResponse saveMakerReview(UUID applicationId, MakerVerifyRequest request, User maker) {
        LoanApplication application = applicationService.require(applicationId);
        assertMakerCanAct(application, maker);
        MakerReview review = new MakerReview();
        review.setApplication(application);
        review.setMaker(maker);
        review.setCustomerInfoVerified(Boolean.TRUE.equals(request.customerInfoVerified()));
        review.setDocumentsVerified(Boolean.TRUE.equals(request.documentsVerified()));
        review.setFinancialsVerified(Boolean.TRUE.equals(request.financialsVerified()));
        review.setRemarks(request.remarks());
        makerReviewRepository.save(review);
        return MakerReviewResponse.from(review);
    }

    @Transactional
    public ApplicationDetailResponse requestInformation(UUID applicationId, String remarks, String missing, User maker) {
        LoanApplication application = applicationService.require(applicationId);
        assertMakerCanAct(application, maker);
        MakerReview review = new MakerReview();
        review.setApplication(application);
        review.setMaker(maker);
        review.setRemarks(remarks);
        review.setMissingInformation(missing);
        makerReviewRepository.save(review);
        applicationService.transition(application, ApplicationStatus.INFO_REQUESTED, maker, remarks);
        notificationService.notify(
                application.getCustomer(),
                "Additional information required",
                "Application " + application.getApplicationNumber() + " needs more information: " + remarks,
                "INFO_REQUEST",
                application
        );
        return ApplicationDetailResponse.from(application);
    }

    @Transactional
    public ApplicationDetailResponse sendToChecker(UUID applicationId, String remarks, User maker) {
        LoanApplication application = applicationService.require(applicationId);
        assertMakerCanAct(application, maker);
        boolean docsOk = documentService.entities(applicationId).stream()
                .allMatch(doc -> doc.getVerificationStatus() == DocumentVerificationStatus.VERIFIED);
        if (!docsOk) {
            throw new BusinessException("All documents must be verified before sending to checker");
        }
        MakerReview latest = makerReviewRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new BusinessException("Complete maker verification before sending to checker"));
        if (!latest.isCustomerInfoVerified() || !latest.isDocumentsVerified() || !latest.isFinancialsVerified()) {
            throw new BusinessException("Customer information, documents and financials must all be verified");
        }
        applicationService.transition(application, ApplicationStatus.MAKER_VERIFIED, maker, remarks);
        applicationService.transition(application, ApplicationStatus.CHECKER_REVIEW, maker, "Forwarded to checker");
        notificationService.notify(
                application.getCustomer(),
                "Application under checker review",
                "Application " + application.getApplicationNumber() + " has been verified by the maker and sent for approval.",
                "APPLICATION_STATUS",
                application
        );
        return ApplicationDetailResponse.from(application);
    }

    @Transactional
    public ApplicationDetailResponse approve(UUID applicationId, String remarks, User checker) {
        LoanApplication application = requireCheckerApplication(applicationId, checker);
        CheckerReview review = new CheckerReview();
        review.setApplication(application);
        review.setChecker(checker);
        review.setDecision(CheckerDecision.APPROVED);
        review.setRemarks(remarks);
        checkerReviewRepository.save(review);
        application.setAssignedChecker(checker);
        application.setDecidedAt(Instant.now());
        applicationService.transition(application, ApplicationStatus.APPROVED, checker, remarks);
        notificationService.notify(
                application.getCustomer(),
                "Loan approved",
                "Congratulations. Application " + application.getApplicationNumber() + " has been approved.",
                "APPLICATION_STATUS",
                application
        );
        return ApplicationDetailResponse.from(application);
    }

    @Transactional
    public ApplicationDetailResponse reject(UUID applicationId, String reason, String remarks, User checker) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A rejection reason is mandatory");
        }
        LoanApplication application = requireCheckerApplication(applicationId, checker);
        CheckerReview review = new CheckerReview();
        review.setApplication(application);
        review.setChecker(checker);
        review.setDecision(CheckerDecision.REJECTED);
        review.setReason(reason);
        review.setRemarks(remarks);
        checkerReviewRepository.save(review);
        application.setAssignedChecker(checker);
        application.setDecidedAt(Instant.now());
        applicationService.transition(application, ApplicationStatus.REJECTED, checker, reason);
        notificationService.notify(
                application.getCustomer(),
                "Loan rejected",
                "Application " + application.getApplicationNumber() + " was rejected. Reason: " + reason,
                "APPLICATION_STATUS",
                application
        );
        return ApplicationDetailResponse.from(application);
    }

    @Transactional
    public ApplicationDetailResponse returnToMaker(UUID applicationId, String remarks, User checker) {
        LoanApplication application = requireCheckerApplication(applicationId, checker);
        CheckerReview review = new CheckerReview();
        review.setApplication(application);
        review.setChecker(checker);
        review.setDecision(CheckerDecision.RETURNED);
        review.setRemarks(remarks);
        checkerReviewRepository.save(review);
        application.setAssignedChecker(checker);
        applicationService.transition(application, ApplicationStatus.RETURNED_TO_MAKER, checker, remarks);
        applicationService.transition(application, ApplicationStatus.MAKER_REVIEW, checker, "Returned to maker");
        return ApplicationDetailResponse.from(application);
    }

    @Transactional(readOnly = true)
    public EligibilityResponse eligibility(UUID applicationId, User actor) {
        LoanApplication application = applicationService.requireVisibleTo(applicationId, actor);
        return eligibilityService.evaluate(application);
    }

    @Transactional(readOnly = true)
    public List<MakerReviewResponse> makerHistory(UUID applicationId, User actor) {
        applicationService.requireVisibleTo(applicationId, actor);
        return makerReviewRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream().map(MakerReviewResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CheckerReviewResponse> checkerHistory(UUID applicationId, User actor) {
        applicationService.requireVisibleTo(applicationId, actor);
        return checkerReviewRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream().map(CheckerReviewResponse::from).toList();
    }

    private LoanApplication requireCheckerApplication(UUID applicationId, User checker) {
        LoanApplication application = applicationService.require(applicationId);
        if (application.getStatus() != ApplicationStatus.CHECKER_REVIEW
                && application.getStatus() != ApplicationStatus.MAKER_VERIFIED) {
            throw new BusinessException("Application is not waiting for checker decision");
        }
        if (application.getAssignedMaker() != null
                && application.getAssignedMaker().getId().equals(checker.getId())) {
            throw new ForbiddenException("Maker-checker violation: you cannot approve an application you verified");
        }
        makerReviewRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
                .filter(review -> review.getMaker().getId().equals(checker.getId()))
                .ifPresent(review -> {
                    throw new ForbiddenException("Maker-checker violation: you previously acted as maker on this application");
                });
        return application;
    }

    private void assertMakerCanAct(LoanApplication application, User maker) {
        if (application.getStatus() != ApplicationStatus.MAKER_REVIEW
                && application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.RETURNED_TO_MAKER) {
            throw new BusinessException("Application is not in maker review");
        }
        if (application.getAssignedMaker() != null
                && !application.getAssignedMaker().getId().equals(maker.getId())) {
            throw new ForbiddenException("Application is assigned to another maker");
        }
        if (application.getAssignedMaker() == null) {
            application.setAssignedMaker(maker);
            if (application.getStatus() == ApplicationStatus.SUBMITTED
                    || application.getStatus() == ApplicationStatus.RETURNED_TO_MAKER) {
                applicationService.transition(application, ApplicationStatus.MAKER_REVIEW, maker, "Maker started review");
            }
        }
    }

    private String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
