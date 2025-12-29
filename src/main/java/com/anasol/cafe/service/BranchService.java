package com.anasol.cafe.service;

import com.anasol.cafe.dto.BranchResponse;
import com.anasol.cafe.dto.CreateBranchRequest;
import com.anasol.cafe.dto.PageResponse;
import com.anasol.cafe.entity.Branch;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchResponse createBranch(CreateBranchRequest request) {

        if (branchRepository.existsByBranchCode(request.branchCode)) {
            log.warn("Branch code already exists: {}", request.branchCode);
            throw new IllegalStateException("Branch code already exists");
        }

        Branch branch = new Branch();
        branch.setBranchCode(request.branchCode);
        branch.setBranchName(request.branchName);
        branch.setAddress(request.address);
        branch.setActive(true);

        Branch saved = branchRepository.save(branch);
        log.info("Branch created: {}", saved.getBranchCode());

        return toResponse(saved);
    }

    public BranchResponse updateBranchStatus(Long branchId, boolean active) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> {
                    log.error("Branch not found: {}", branchId);
                    return new ResourceNotFoundException(
                            "Branch not found with id: " + branchId
                    );
                });

        branch.setActive(active);
        log.info("Branch {} status updated to {}", branch.getBranchCode(), active);

        return toResponse(branch);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllActiveBranches() {

        List<Branch> branches = branchRepository.findByActiveTrue();

        if (branches.isEmpty()) {
            log.warn("No active branches found");
            return List.of();
        }

        log.info("Fetched {} active branches", branches.size());

        return branches.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranchesForAdmin() {

        List<Branch> branches = branchRepository.findAllByOrderByBranchNameAsc();

        if (branches.isEmpty()) {
            log.warn("No branches found");
            return List.of();
        }

        log.info("Fetched {} branches for admin", branches.size());

        return branches.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> getAllActiveBranches(Pageable pageable) {

        Page<Branch> page = branchRepository.findByActiveTrue(pageable);

        Page<BranchResponse> dtoPage = page.map(this::toResponse);

        log.info("Fetched {} active branches (page {}, size {})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getSize());

        return PageResponse.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> getAllBranchesForAdmin(Pageable pageable) {

        Page<Branch> page =
                branchRepository.findAllByOrderByBranchNameAsc(pageable);

        Page<BranchResponse> dtoPage = page.map(this::toResponse);

        log.info("Fetched {} branches for admin (page {}, size {})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getSize());

        return PageResponse.of(dtoPage);
    }

    private BranchResponse toResponse(Branch branch) {
        BranchResponse dto = new BranchResponse();
        dto.setId(branch.getId());
        dto.setBranchCode(branch.getBranchCode());
        dto.setBranchName(branch.getBranchName());
        dto.setAddress(branch.getAddress());
        dto.setActive(branch.isActive());
        return dto;
    }
}
