package com.wan.batch.example;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class ExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExampleApplication.class, args);
	}

}
