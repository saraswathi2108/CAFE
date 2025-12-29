package com.anasol.cafe.repository;

import com.anasol.cafe.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByBranchCode(String branchCode);

    // Pagination
    Page<Branch> findByActiveTrue(Pageable pageable);

    Page<Branch> findAllByOrderByBranchNameAsc(Pageable pageable);

    List<Branch> findByActiveTrue();

    List<Branch> findAllByOrderByBranchNameAsc();
}
