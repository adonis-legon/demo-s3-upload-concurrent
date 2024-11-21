package com.example.demo_s3_upload_concurrent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
		assertDoesNotThrow(() -> randomFileUploaderService.uploadFilesFromDirectory("files-to-upload-test"));
	}
}
