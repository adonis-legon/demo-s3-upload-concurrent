package com.example.demo_s3_upload_concurrent;

import java.util.Arrays;
import java.util.List;

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
			// Upload files and get the list of uploaded file names
			List<String> uploadedFiles = randomFileUploaderService.uploadFilesFromDirectory("files-to-upload");

			// Download the uploaded files to a fixed directory
			if (!uploadedFiles.isEmpty()) {
				System.out.println("Starting download of uploaded files...");
				randomFileUploaderService.downloadFilesFromS3(uploadedFiles, "files-downloaded");
				System.out.println("Download completed!");
			} else {
				System.out.println("No files were uploaded, skipping download.");
			}
		}
	}
}
