package com.loanpro.workflow;

import com.loanpro.common.exception.ForbiddenException;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.application.repository.CheckerReviewRepository;
import com.loanpro.modules.application.repository.LoanApplicationRepository;
import com.loanpro.modules.application.repository.MakerReviewRepository;
import com.loanpro.modules.application.service.LoanApplicationService;
import com.loanpro.modules.document.service.DocumentService;
import com.loanpro.modules.identity.domain.Role;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.notification.service.NotificationService;
import com.loanpro.modules.workflow.service.EligibilityService;
import com.loanpro.modules.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakerCheckerRuleTest {

    @Mock private LoanApplicationRepository applicationRepository;
    @Mock private LoanApplicationService applicationService;
    @Mock private MakerReviewRepository makerReviewRepository;
    @Mock private CheckerReviewRepository checkerReviewRepository;
    @Mock private DocumentService documentService;
    @Mock private EligibilityService eligibilityService;
    @Mock private NotificationService notificationService;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(
                applicationRepository,
                applicationService,
                makerReviewRepository,
                checkerReviewRepository,
                documentService,
                eligibilityService,
                notificationService
        );
    }

    @Test
    void checkerCannotApproveApplicationTheyVerifiedAsMaker() {
        User staff = userWith(RoleName.MAKER);
        staff.getRoles().add(role(RoleName.CHECKER));

        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setStatus(ApplicationStatus.CHECKER_REVIEW);
        application.setAssignedMaker(staff);

        when(applicationService.require(any())).thenReturn(application);

        assertThrows(ForbiddenException.class,
                () -> workflowService.approve(application.getId(), "ok", staff));
    }

    private User userWith(RoleName roleName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("staff@loanpro.com");
        user.setFirstName("Staff");
        user.setLastName("User");
        user.getRoles().add(role(roleName));
        user.setRole(roleName);
        return user;
    }

    private Role role(RoleName name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
