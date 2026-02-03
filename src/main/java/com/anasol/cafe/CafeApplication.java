package com.anasol.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CafeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CafeApplication.class, args);
	}

}
















