package com.example.demo_s3_upload_concurrent;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class DemoS3UploadConcurrentApplication implements CommandLineRunner {

	@Autowired
	private RandomFileUploaderService randomFileUploaderService;

	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(DemoS3UploadConcurrentApplication.class, args);
	}

	@Override
	public void run(String... args) {
		if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
			randomFileUploaderService.uploadFilesFromDirectory("files-to-upload");
		}
	}
}
