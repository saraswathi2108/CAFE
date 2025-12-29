package com.anasol.cafe.service;


import com.anasol.cafe.dto.ChangePasswordRequest;
import com.anasol.cafe.dto.LoginRequest;
import com.anasol.cafe.dto.LoginResponse;
import com.anasol.cafe.entity.User;
import com.anasol.cafe.exceptions.InvalidCredentialsException;
import com.anasol.cafe.exceptions.UserDisabledException;
import com.anasol.cafe.exceptions.UserNotFoundException;
import com.anasol.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email)
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password")
                );

        if (!user.isActive()) {
            throw new UserDisabledException("User account is disabled");
        }

        if (user.getBranch() != null && !user.getBranch().isActive()) {
            throw new UserDisabledException("Branch is inactive");
        }

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        LoginResponse response = new LoginResponse();
        response.accessToken = jwtService.generateToken(user);
        response.role = user.getRole().name();
        response.firstLogin = user.isFirstLogin();

        return response;
    }

    public void changePassword(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        if (!user.isActive()) {
            throw new UserDisabledException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword, user.getPassword())) {
            throw new InvalidCredentialsException(
                    "New password must be different from old password"
            );
        }

        user.setPassword(passwordEncoder.encode(request.newPassword));
        user.setFirstLogin(false);
    }
}