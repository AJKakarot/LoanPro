package com.loanpro.modules.dashboard.service;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.dto.ApplicationSummaryResponse;
import com.loanpro.modules.application.repository.LoanApplicationRepository;
import com.loanpro.modules.dashboard.dto.DashboardStatsResponse;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final LoanApplicationRepository applicationRepository;

    public DashboardService(LoanApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats(User actor) {
        var recentPage = actor.hasRole(RoleName.CUSTOMER) && !actor.hasRole(RoleName.ADMIN)
                ? applicationRepository.findCustomerApplications(actor.getId(), null, "", PageRequest.of(0, 8))
                : applicationRepository.searchAll(null, "", PageRequest.of(0, 8));
        List<ApplicationSummaryResponse> recent = recentPage.map(ApplicationSummaryResponse::from).getContent();

        if (actor.hasRole(RoleName.CUSTOMER) && !hasStaffRole(actor)) {
            var mine = applicationRepository.findCustomerApplications(actor.getId(), null, "", PageRequest.of(0, 500));
            long total = mine.getTotalElements();
            long pending = mine.getContent().stream().filter(a ->
                    a.getStatus() == ApplicationStatus.DRAFT
                            || a.getStatus() == ApplicationStatus.SUBMITTED
                            || a.getStatus() == ApplicationStatus.INFO_REQUESTED).count();
            long maker = mine.getContent().stream().filter(a ->
                    a.getStatus() == ApplicationStatus.MAKER_REVIEW
                            || a.getStatus() == ApplicationStatus.RETURNED_TO_MAKER).count();
            long checker = mine.getContent().stream().filter(a ->
                    a.getStatus() == ApplicationStatus.CHECKER_REVIEW
                            || a.getStatus() == ApplicationStatus.MAKER_VERIFIED).count();
            long approved = mine.getContent().stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
            long rejected = mine.getContent().stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count();
            var amount = mine.getContent().stream()
                    .map(a -> a.getRequestedAmount())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            var approvedAmount = mine.getContent().stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.APPROVED)
                    .map(a -> a.getRequestedAmount())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            return new DashboardStatsResponse(total, pending, maker, checker, approved, rejected, amount, approvedAmount, recent);
        }

        return new DashboardStatsResponse(
                applicationRepository.count(),
                applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)
                        + applicationRepository.countByStatus(ApplicationStatus.INFO_REQUESTED)
                        + applicationRepository.countByStatus(ApplicationStatus.DRAFT),
                applicationRepository.countByStatus(ApplicationStatus.MAKER_REVIEW)
                        + applicationRepository.countByStatus(ApplicationStatus.RETURNED_TO_MAKER),
                applicationRepository.countByStatus(ApplicationStatus.CHECKER_REVIEW)
                        + applicationRepository.countByStatus(ApplicationStatus.MAKER_VERIFIED),
                applicationRepository.countByStatus(ApplicationStatus.APPROVED),
                applicationRepository.countByStatus(ApplicationStatus.REJECTED),
                applicationRepository.sumRequestedAmount(),
                applicationRepository.sumRequestedAmountByStatus(ApplicationStatus.APPROVED),
                recent
        );
    }

    private boolean hasStaffRole(User actor) {
        return actor.hasRole(RoleName.ADMIN) || actor.hasRole(RoleName.MAKER) || actor.hasRole(RoleName.CHECKER);
    }
}
