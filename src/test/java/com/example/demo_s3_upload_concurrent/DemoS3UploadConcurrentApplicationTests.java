package com.example.demo_s3_upload_concurrent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DemoS3UploadConcurrentApplicationTests {

	@Autowired
	private RandomFileUploaderService randomFileUploaderService;

	@Test
	void whenUploadingMultipleFilesToS3_ItMustWork() {
		assertDoesNotThrow(() -> {
			List<String> uploadedFiles = randomFileUploaderService.uploadFilesFromDirectory("files-to-upload-test");
			// Verify that files were uploaded
			assertNotNull(uploadedFiles);
			assertFalse(uploadedFiles.isEmpty());
		});
	}
}
