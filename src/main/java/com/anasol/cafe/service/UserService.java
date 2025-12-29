package com.anasol.cafe.service;

import com.anasol.cafe.dto.CreateUserRequest;
import com.anasol.cafe.dto.CreateUserResponse;
import com.anasol.cafe.entity.Branch;
import com.anasol.cafe.entity.Role;
import com.anasol.cafe.entity.User;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.UserAlreadyExistsException;
import com.anasol.cafe.repository.BranchRepository;
import com.anasol.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserResponse createUser(CreateUserRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            log.error("Unauthorized attempt to create user");
            throw new RuntimeException("Unauthorized");
        }

        String currentRole = auth.getAuthorities().iterator().next().getAuthority();

        User creator = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        if (currentRole.equals("ROLE_STAFF")) {
            log.warn("Staff tried to create user");
            throw new RuntimeException("Staff cannot create users");
        }

        if (request.role == Role.ADMIN && !currentRole.equals("ROLE_ADMIN")) {
            log.warn("Non-admin attempted to create admin");
            throw new RuntimeException("Only admin can create admin");
        }

        if (currentRole.equals("ROLE_MANAGER") && request.role == Role.MANAGER) {
            log.warn("Manager attempted to create manager");
            throw new RuntimeException("Manager cannot create manager");
        }

        if (currentRole.equals("ROLE_MANAGER") && request.role != Role.STAFF) {
            log.warn("Manager attempted to create invalid role");
            throw new RuntimeException("Manager can create only staff");
        }

        if (userRepository.existsByEmail(request.email.toLowerCase())) {
            log.warn("User already exists with email {}", request.email);
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.email
            );
        }

        Branch branch = null;

        if (request.role != Role.ADMIN) {
            if (currentRole.equals("ROLE_MANAGER")) {
                branch = creator.getBranch();
                if (branch == null) {
                    log.error("Manager has no branch assigned");
                    throw new RuntimeException("Manager has no branch assigned");
                }
            } else {
                branch = branchRepository.findById(request.branchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
                if (!branch.isActive()) {
                    log.warn("Inactive branch used");
                    throw new RuntimeException("Branch is inactive");
                }
            }
        }

        User user = new User();
        user.setEmail(request.email.toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole(request.role);
        user.setBranch(branch);
        user.setFirstLogin(true);
        user.setActive(true);

        User saved = userRepository.save(user);
        log.info("User created {}", saved.getEmail());

        return mapToResponse(saved);
    }

    public List<CreateUserResponse> getAllManagers() {

        List<User> managers = userRepository.findByRole(Role.MANAGER);

        if (managers.isEmpty()) {
            log.warn("No managers found");
            return List.of();
        }

        return managers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CreateUserResponse> getStaff() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Unauthorized access");
            throw new RuntimeException("Unauthorized");
        }

        User currentUser = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> staff;

        if (currentUser.getRole() == Role.ADMIN) {
            staff = userRepository.findByRole(Role.STAFF);
        } else if (currentUser.getRole() == Role.MANAGER) {
            if (currentUser.getBranch() == null) {
                log.error("Manager has no branch");
                throw new RuntimeException("Manager has no branch assigned");
            }
            staff = userRepository.findByRoleAndBranchId(
                    Role.STAFF,
                    currentUser.getBranch().getId()
            );
        } else {
            log.warn("Access denied");
            throw new RuntimeException("Access denied");
        }

        if (staff.isEmpty()) {
            log.warn("No staff found");
            return List.of();
        }

        return staff.stream().map(this::mapToResponse).toList();
    }

    public List<CreateUserResponse> getStaffByBranchId(Long branchId) {

        List<User> staff = userRepository.findByRoleAndBranchId(Role.STAFF, branchId);

        if (staff.isEmpty()) {
            log.warn("No staff found for branch {}", branchId);
            return List.of();
        }

        return staff.stream().map(this::mapToResponse).toList();
    }

    public List<CreateUserResponse> getManagersByBranchId(Long branchId) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        List<User> managers = userRepository.findByRoleAndBranchId(Role.MANAGER, branch.getId());

        if (managers.isEmpty()) {
            log.warn("No managers found for branch {}", branchId);
            return List.of();
        }

        return managers.stream().map(this::mapToResponse).toList();
    }

    public CreateUserResponse updateUserStatus(Long userId, boolean active) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Unauthorized access");
            throw new RuntimeException("Unauthorized");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        targetUser.setActive(active);
        userRepository.save(targetUser);

        log.info("User status updated {}", targetUser.getEmail());
        return mapToResponse(targetUser);
    }

    private CreateUserResponse mapToResponse(User user) {
        CreateUserResponse response = new CreateUserResponse();
        response.id = user.getId();
        response.email = user.getEmail();
        response.role = user.getRole().name();
        response.branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        response.firstLogin = user.isFirstLogin();
        response.active = user.isActive();
        return response;
    }
}