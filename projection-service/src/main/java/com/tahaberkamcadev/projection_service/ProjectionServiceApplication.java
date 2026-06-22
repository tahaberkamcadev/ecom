package com.tahaberkamcadev.projection_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ProjectionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectionServiceApplication.class, args);
	}

}
