package com.anasol.cafe.controller;


import com.anasol.cafe.dto.BranchResponse;
import com.anasol.cafe.dto.CreateBranchRequest;
import com.anasol.cafe.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class BranchController {

    private final BranchService branchService;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/branches")
    public BranchResponse createBranch(
            @Valid @RequestBody CreateBranchRequest request
    ) {
        return branchService.createBranch(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/branches/{branchId}/status")
    public BranchResponse updateBranchStatus(
            @PathVariable Long branchId,
            @RequestParam boolean active
    ) {
        return branchService.updateBranchStatus(branchId, active);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/branches")
    public List<BranchResponse> getActiveBranches() {
        return branchService.getAllActiveBranches();
    }

    @GetMapping("/admin/branches")
    public List<BranchResponse> getAllBranchesForAdmin() {
        return branchService.getAllBranchesForAdmin();
    }

    @PutMapping("/admin/branch/{id}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long id,
            @RequestBody BranchResponse request
    ) {
        return ResponseEntity.ok(branchService.updateById(id, request));
    }

}