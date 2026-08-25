package com.loanpro.modules.identity.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.common.api.PageResponse;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.UserStatus;
import com.loanpro.modules.identity.dto.CreateUserRequest;
import com.loanpro.modules.identity.dto.RoleResponse;
import com.loanpro.modules.identity.dto.UpdateUserRequest;
import com.loanpro.modules.identity.dto.UserResponse;
import com.loanpro.modules.identity.service.UserAdminService;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final CurrentUserService currentUserService;

    public AdminUserController(UserAdminService userAdminService, CurrentUserService currentUserService) {
        this.userAdminService = userAdminService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleName role,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.ok(userAdminService.search(search, status, role, pageable));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("User created", userAdminService.create(request, currentUserService.require(principal)));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(userAdminService.get(id));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(userAdminService.update(id, request, currentUserService.require(principal)));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> roles() {
        return ApiResponse.ok(userAdminService.roles());
    }
}
