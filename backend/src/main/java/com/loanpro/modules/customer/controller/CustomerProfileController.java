package com.loanpro.modules.customer.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.modules.customer.dto.CustomerProfileResponse;
import com.loanpro.modules.customer.dto.UpdateProfileRequest;
import com.loanpro.modules.customer.service.CustomerProfileService;
import com.loanpro.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping
    public ApiResponse<CustomerProfileResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(customerProfileService.get(principal.getId()));
    }

    @PutMapping
    public ApiResponse<CustomerProfileResponse> update(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Profile updated", customerProfileService.update(principal.getId(), request));
    }
}
