package com.loanpro.modules.application.service;

import com.loanpro.common.exception.BusinessException;
import com.loanpro.common.exception.ForbiddenException;
import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.ApplicationStatusHistory;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.application.dto.UpsertApplicationRequest;
import com.loanpro.modules.application.repository.ApplicationStatusHistoryRepository;
import com.loanpro.modules.application.repository.LoanApplicationRepository;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.catalog.domain.LoanProduct;
import com.loanpro.modules.catalog.repository.LoanProductRepository;
import com.loanpro.modules.customer.domain.CustomerProfile;
import com.loanpro.modules.customer.repository.CustomerProfileRepository;
import com.loanpro.modules.document.repository.LoanDocumentRepository;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.repository.UserRepository;
import com.loanpro.infrastructure.numbering.ApplicationNumberGenerator;
import com.loanpro.modules.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class LoanApplicationService {

    private static final Set<ApplicationStatus> CUSTOMER_EDITABLE = EnumSet.of(
            ApplicationStatus.DRAFT,
            ApplicationStatus.INFO_REQUESTED
    );

    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository loanProductRepository;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final LoanDocumentRepository documentRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationNumberGenerator numberGenerator;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public LoanApplicationService(
            LoanApplicationRepository applicationRepository,
            LoanProductRepository loanProductRepository,
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            LoanDocumentRepository documentRepository,
            ApplicationStatusHistoryRepository historyRepository,
            ApplicationNumberGenerator numberGenerator,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.applicationRepository = applicationRepository;
        this.loanProductRepository = loanProductRepository;
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.numberGenerator = numberGenerator;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public LoanApplication create(UUID customerId, UpsertApplicationRequest request) {
        User customer = requireUser(customerId);
        LoanProduct product = requireActiveProduct(request.loanProductId());
        validateAgainstProduct(product, request);
        LoanApplication application = new LoanApplication();
        application.setApplicationNumber(numberGenerator.next());
        application.setCustomer(customer);
        application.setLoanProduct(product);
        application.setInterestRate(product.getInterestRate());
        application.setProcessingFeePercent(product.getProcessingFeePercent());
        application.setStatus(ApplicationStatus.DRAFT);
        applySnapshot(application, request, customer);
        applicationRepository.save(application);
        recordHistory(application, null, ApplicationStatus.DRAFT, customer, "Application created");
        auditService.record(customer, "APPLICATION_CREATED", "LoanApplication", application.getId(),
                application, null, ApplicationStatus.DRAFT, application.getApplicationNumber());
        return application;
    }

    @Transactional
    public LoanApplication update(UUID applicationId, UUID actorId, UpsertApplicationRequest request) {
        LoanApplication application = require(applicationId);
        User actor = requireUser(actorId);
        assertCustomerOwner(application, actor);
        if (!CUSTOMER_EDITABLE.contains(application.getStatus())) {
            throw new BusinessException("Application can only be edited while in draft or when information is requested");
        }
        LoanProduct product = requireActiveProduct(request.loanProductId());
        validateAgainstProduct(product, request);
        application.setLoanProduct(product);
        application.setInterestRate(product.getInterestRate());
        application.setProcessingFeePercent(product.getProcessingFeePercent());
        applySnapshot(application, request, actor);
        auditService.record(actor, "APPLICATION_UPDATED", "LoanApplication", application.getId(),
                application, application.getStatus(), application.getStatus(), application.getApplicationNumber());
        return application;
    }

    @Transactional
    public LoanApplication submit(UUID applicationId, UUID actorId) {
        LoanApplication application = require(applicationId);
        User actor = requireUser(actorId);
        assertCustomerOwner(application, actor);
        if (!CUSTOMER_EDITABLE.contains(application.getStatus()) && application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("Application cannot be submitted from the current status");
        }
        validateComplete(application);
        if (documentRepository.countByApplicationId(application.getId()) == 0) {
            throw new BusinessException("Upload at least one supporting document before submitting");
        }
        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = ApplicationStatus.SUBMITTED;
        application.setStatus(to);
        application.setSubmittedAt(Instant.now());
        recordHistory(application, from, to, actor, "Customer submitted application");
        auditService.record(actor, "APPLICATION_SUBMITTED", "LoanApplication", application.getId(),
                application, from, to, application.getApplicationNumber());
        notificationService.notify(
                actor,
                "Application submitted",
                "Your application " + application.getApplicationNumber() + " has been submitted for review.",
                "APPLICATION_STATUS",
                application
        );
        return application;
    }

    @Transactional(readOnly = true)
    public LoanApplication require(UUID id) {
        return applicationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found"));
    }

    @Transactional(readOnly = true)
    public LoanApplication requireVisibleTo(UUID id, User actor) {
        LoanApplication application = require(id);
        if (actor.hasRole(RoleName.ADMIN) || actor.hasRole(RoleName.MAKER) || actor.hasRole(RoleName.CHECKER)) {
            return application;
        }
        if (!application.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("You can only access your own applications");
        }
        return application;
    }

    public void transition(LoanApplication application, ApplicationStatus to, User actor, String remarks) {
        ApplicationStatus from = application.getStatus();
        application.setStatus(to);
        recordHistory(application, from, to, actor, remarks);
        auditService.record(actor, "STATUS_CHANGED", "LoanApplication", application.getId(),
                application, from, to, remarks);
    }

    public User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void applySnapshot(LoanApplication application, UpsertApplicationRequest request, User customer) {
        application.setRequestedAmount(request.requestedAmount());
        application.setTenureMonths(request.tenureMonths());
        application.setPurpose(request.purpose().trim());
        application.setFullName(firstNonBlank(request.fullName(), customer.getFullName()));
        application.setDateOfBirth(request.dateOfBirth());
        application.setGender(request.gender());
        application.setNationalId(request.nationalId());
        application.setPhone(firstNonBlank(request.phone(), customer.getPhone()));
        application.setEmail(firstNonBlank(request.email(), customer.getEmail()));
        application.setAddressLine(request.addressLine());
        application.setCity(request.city());
        application.setState(request.state());
        application.setPostalCode(request.postalCode());
        application.setEmploymentType(request.employmentType());
        application.setEmployerName(request.employerName());
        application.setDesignation(request.designation());
        application.setYearsEmployed(request.yearsEmployed());
        application.setMonthlyIncome(request.monthlyIncome());
        application.setOtherIncome(request.otherIncome());
        application.setExistingEmis(request.existingEmis());
        application.setMonthlyExpenses(request.monthlyExpenses());

        customerProfileRepository.findByUserId(customer.getId()).ifPresent(profile -> copyMissingFromProfile(application, profile, customer));
    }

    private void copyMissingFromProfile(LoanApplication application, CustomerProfile profile, User customer) {
        if (application.getDateOfBirth() == null) {
            application.setDateOfBirth(profile.getDateOfBirth());
        }
        if (application.getGender() == null) {
            application.setGender(profile.getGender());
        }
        if (application.getNationalId() == null) {
            application.setNationalId(profile.getNationalId());
        }
        if (application.getAddressLine() == null) {
            application.setAddressLine(profile.getAddressLine());
        }
        if (application.getCity() == null) {
            application.setCity(profile.getCity());
        }
        if (application.getState() == null) {
            application.setState(profile.getState());
        }
        if (application.getPostalCode() == null) {
            application.setPostalCode(profile.getPostalCode());
        }
        if (application.getEmploymentType() == null) {
            application.setEmploymentType(profile.getEmploymentType());
        }
        if (application.getEmployerName() == null) {
            application.setEmployerName(profile.getEmployerName());
        }
        if (application.getMonthlyIncome() == null) {
            application.setMonthlyIncome(profile.getMonthlyIncome());
        }
        if (application.getExistingEmis() == null) {
            application.setExistingEmis(profile.getExistingEmis());
        }
        if (application.getFullName() == null) {
            application.setFullName(customer.getFullName());
        }
    }

    private void validateAgainstProduct(LoanProduct product, UpsertApplicationRequest request) {
        if (request.requestedAmount().compareTo(product.getMinAmount()) < 0
                || request.requestedAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("Requested amount is outside the product range");
        }
        if (request.tenureMonths() < product.getMinTenureMonths()
                || request.tenureMonths() > product.getMaxTenureMonths()) {
            throw new BusinessException("Tenure is outside the product range");
        }
    }

    private void validateComplete(LoanApplication application) {
        if (isBlank(application.getFullName()) || isBlank(application.getNationalId())
                || isBlank(application.getAddressLine()) || application.getDateOfBirth() == null
                || application.getEmploymentType() == null || application.getMonthlyIncome() == null) {
            throw new BusinessException("Complete personal, employment and financial details before submitting");
        }
    }

    private LoanProduct requireActiveProduct(UUID id) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));
        if (!product.isActive()) {
            throw new BusinessException("Loan product is not available");
        }
        return product;
    }

    private void assertCustomerOwner(LoanApplication application, User actor) {
        if (!application.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("You can only modify your own applications");
        }
    }

    private void recordHistory(
            LoanApplication application,
            ApplicationStatus from,
            ApplicationStatus to,
            User actor,
            String remarks
    ) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actor);
        history.setRemarks(remarks);
        historyRepository.save(history);
    }

    private String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
