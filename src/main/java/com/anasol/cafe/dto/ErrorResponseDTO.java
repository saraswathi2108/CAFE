package com.anasol.cafe.dto;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class ErrorResponseDTO {
	
	private String message;
	private HttpStatus status;
	private int statusCode;
	private LocalDateTime timestamp;
	
	
	public ErrorResponseDTO(String message, HttpStatus notFound, int statusCode) {
		this.message = message;
		this.status = notFound;
		this.statusCode = statusCode;
		this.timestamp = LocalDateTime.now();
	}
	
}
