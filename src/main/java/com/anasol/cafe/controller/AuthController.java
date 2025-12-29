package com.anasol.cafe.controller;




import com.anasol.cafe.dto.ChangePasswordRequest;
import com.anasol.cafe.dto.LoginRequest;
import com.anasol.cafe.dto.LoginResponse;
import com.anasol.cafe.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }


    @PostMapping("/change-password")
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }

        authService.changePassword(authentication.getName(), request);
    }


}