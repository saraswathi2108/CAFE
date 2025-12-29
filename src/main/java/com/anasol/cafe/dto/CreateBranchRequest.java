package com.anasol.cafe.dto;


import jakarta.validation.constraints.NotBlank;

public class CreateBranchRequest {

    @NotBlank
    public String branchCode;

    @NotBlank
    public String branchName;

    public String address;
}

