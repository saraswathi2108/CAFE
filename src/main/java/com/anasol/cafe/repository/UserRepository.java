package com.anasol.cafe.repository;




import com.anasol.cafe.entity.Role;
import com.anasol.cafe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Optional<User> findByEmail(String email);

    List<User> findByBranch_Id(Long branchId);

    List<User> findByRoleAndBranchId(Role role, Long id);

    List<User> findByRole(Role role);
}