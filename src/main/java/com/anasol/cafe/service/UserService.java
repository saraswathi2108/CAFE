package com.anasol.cafe.service;

import com.anasol.cafe.dto.CreateUserRequest;
import com.anasol.cafe.dto.CreateUserResponse;
import com.anasol.cafe.entity.Branch;
import com.anasol.cafe.entity.Role;
import com.anasol.cafe.entity.User;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.UserAlreadyExistsException;
import com.anasol.cafe.exceptions.UserDisabledException;
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
            throw new UserDisabledException("Unauthorized");
        }

        String currentRole = auth.getAuthorities().iterator().next().getAuthority();

        User creator = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        // Check permissions based on current user's role
        switch (currentRole) {
            case "ROLE_STAFF":
                log.warn("Staff tried to create user");
                throw new UserDisabledException("Staff cannot create users");

            case "ROLE_MANAGER":
                if (request.role == Role.MANAGER) {
                    log.warn("Manager attempted to create manager");
                    throw new UserDisabledException("Manager cannot create manager");
                }
                if (request.role != Role.STAFF) {
                    log.warn("Manager attempted to create invalid role");
                    throw new UserDisabledException("Manager can create only staff");
                }
                break;

            case "ROLE_GODOWN_MANAGER":

                if (request.role == Role.ADMIN || request.role == Role.GODOWN_MANAGER) {
                    log.warn("Godown Manager attempted to create {} role", request.role);
                    throw new UserDisabledException("Godown Manager cannot create " + request.role + " users");
                }
                break;

            case "ROLE_ADMIN":

                break;

            default:
                log.warn("Unknown role attempted to create user");
                throw new UserDisabledException("Unauthorized role");
        }

        if (userRepository.existsByEmail(request.email.toLowerCase())) {
            log.warn("User already exists with email {}", request.email);
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.email
            );
        }

        // ... existing code ...

        Branch branch = null;

// Handle branch assignment based on role
        if (request.role == Role.ADMIN) {
            // Admin doesn't need a branch
            branch = null;
        } else if (currentRole.equals("ROLE_MANAGER")) {
            // Manager can only create staff for their own branch
            branch = creator.getBranch();
            if (branch == null) {
                log.error("Manager has no branch assigned");
                throw new ResourceNotFoundException("Manager has no branch assigned");
            }
        } else if (currentRole.equals("ROLE_GODOWN_MANAGER") && request.role == Role.MANAGER) {
            // Godown Manager creating a Manager - check if branchId is provided
            if (request.branchId != null) {
                branch = branchRepository.findById(request.branchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
                if (!branch.isActive()) {
                    log.warn("Inactive branch used");
                    throw new UserDisabledException("Branch is inactive");
                }
            } else {

                branch = null;
            }
        } else if (request.role != Role.ADMIN) {

            if (request.role == Role.GODOWN_MANAGER && request.branchId == null) {

                branch = null;
            } else if (request.branchId != null) {
                // For other roles, branch is required
                branch = branchRepository.findById(request.branchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
                if (!branch.isActive()) {
                    log.warn("Inactive branch used");
                    throw new UserDisabledException("Branch is inactive");
                }
            } else {
                // Branch is required for MANAGER and STAFF roles
                throw new ResourceNotFoundException("Branch ID is required for " + request.role + " role");
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

    public List<CreateUserResponse> getAllGodownManagers() {
        List<User> godownManagers = userRepository.findByRole(Role.GODOWN_MANAGER);

        if (godownManagers.isEmpty()) {
            log.warn("No godown managers found");
            return List.of();
        }

        return godownManagers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CreateUserResponse> getStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Unauthorized access");
            throw new UserDisabledException("Unauthorized");
        }

        User currentUser = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<User> staff;

        switch (currentUser.getRole()) {
            case ADMIN:
            case GODOWN_MANAGER:
                staff = userRepository.findByRole(Role.STAFF);
                break;

            case MANAGER:
                if (currentUser.getBranch() == null) {
                    log.error("Manager has no branch");
                    throw new ResourceNotFoundException("Manager has no branch assigned");
                }
                staff = userRepository.findByRoleAndBranchId(
                        Role.STAFF,
                        currentUser.getBranch().getId()
                );
                break;

            default:
                log.warn("Access denied");
                throw new UserDisabledException("Access denied");
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

    public List<CreateUserResponse> getUsersByRole(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Unauthorized access");
            throw new UserDisabledException("Unauthorized");
        }

        User currentUser = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check permissions
        if (currentUser.getRole() == Role.STAFF) {
            throw new UserDisabledException("Access denied");
        }

        List<User> users = userRepository.findByRole(role);

        if (users.isEmpty()) {
            log.warn("No users found with role {}", role);
            return List.of();
        }

        return users.stream().map(this::mapToResponse).toList();
    }

    public CreateUserResponse updateUserStatus(Long userId, boolean active) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Unauthorized access");
            throw new UserDisabledException("Unauthorized");
        }

        User currentUser = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Permission checks
        if (currentUser.getRole() == Role.MANAGER) {
            // Manager can only update staff in their branch
            if (targetUser.getRole() != Role.STAFF ||
                    currentUser.getBranch() == null ||
                    targetUser.getBranch() == null ||
                    !currentUser.getBranch().getId().equals(targetUser.getBranch().getId())) {
                throw new UserDisabledException("Manager can only update staff in their branch");
            }
        } else if (currentUser.getRole() == Role.GODOWN_MANAGER) {
            // Godown Manager can update Staff and Managers, but not Admin or other Godown Managers
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.GODOWN_MANAGER) {
                throw new UserDisabledException("Godown Manager cannot update " + targetUser.getRole());
            }
        }
        // Admin can update anyone

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

    // New method to get all users based on current user's role
    public List<CreateUserResponse> getAllUsersForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Unauthorized access");
            throw new UserDisabledException("Unauthorized");
        }

        User currentUser = userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<User> users;

        switch (currentUser.getRole()) {
            case ADMIN:
                users = userRepository.findAll();
                break;

            case GODOWN_MANAGER:
                // Godown Manager can see all except Admin
                users = userRepository.findByRoleNot(Role.ADMIN);
                break;

            case MANAGER:
                // Manager can see only staff in their branch
                if (currentUser.getBranch() == null) {
                    throw new ResourceNotFoundException("Manager has no branch assigned");
                }
                users = userRepository.findByRoleAndBranchId(
                        Role.STAFF,
                        currentUser.getBranch().getId()
                );
                break;

            default:
                throw new UserDisabledException("Access denied");
        }

        return users.stream().map(this::mapToResponse).toList();
    }
}