package com.anasol.cafe.controller;


import com.anasol.cafe.dto.CreateUserRequest;
import com.anasol.cafe.dto.CreateUserResponse;
import com.anasol.cafe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/admin/users")
    public CreateUserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/managers")
    public List<CreateUserResponse> getAllManagers() {
        return userService.getAllManagers();
    }
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/staff")
    public List<CreateUserResponse> getStaff() {
        return userService.getStaff();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/branches/{branchId}/staff")
    public List<CreateUserResponse> getStaffByBranch(
            @PathVariable Long branchId
    ) {
        return userService.getStaffByBranchId(branchId);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/branches/{branchId}/managers")
    public List<CreateUserResponse> getManagersByBranch(
            @PathVariable Long branchId
    ) {
        return userService.getManagersByBranchId(branchId);
    }


    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/users/{userId}/status")
    public CreateUserResponse updateUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean active
    ) {
        return userService.updateUserStatus(userId, active);
    }





}