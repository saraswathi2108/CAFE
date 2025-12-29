package com.anasol.cafe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ChangePasswordRequest {

    @NotBlank
    public String oldPassword;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "New password must contain at least 1 uppercase letter, 1 number, 1 special character and be minimum 8 characters"
    )
    public String newPassword;
}

