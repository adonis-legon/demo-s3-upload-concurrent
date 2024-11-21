package com.example.demo_s3_upload_concurrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class RandomFileUploaderService {

    private final AppConfig appConfig;

    public RandomFileUploaderService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void uploadFilesFromDirectory(String directory) {
        Path tempDir = null;
        S3VirtualThreadUploader uploader = new S3VirtualThreadUploader(this.appConfig);

        try {
            // Create temporary directory for files
            String tempDirPrefix = directory + "-temp-";
            tempDir = Files.createTempDirectory(tempDirPrefix);

            // Generate random files
            List<Path> filesToUpload = generateRandomFilesWithVaryingSizes(tempDir,
                    Integer.parseInt(this.appConfig.getTotalTemporalFiles()));

            long startTime = System.currentTimeMillis();
            uploader.uploadFilesConcurrently(filesToUpload);
            long endTime = System.currentTimeMillis();

            System.out.printf("Uploaded %d files in %d ms%n", filesToUpload.size(), (endTime - startTime));
        } catch (Exception e) {
            System.err.println("Error during batch upload: " + e.getMessage());
            e.printStackTrace();
        } finally {
            uploader.cleanUp();
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private static List<Path> generateRandomFilesWithVaryingSizes(Path directory, int numberOfFiles)
            throws IOException {
        List<Path> files = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        int[] possibleSizes = { 1024, 3 * 1024, 5 * 1024, 10 * 1024 };

        for (int i = 0; i < numberOfFiles; i++) {
            int fileSize = possibleSizes[random.nextInt(possibleSizes.length)];
            byte[] buffer = new byte[fileSize];
            random.nextBytes(buffer);

            Path filePath = directory
                    .resolve(String.format("test_file_%s_%dKB.dat", UUID.randomUUID().toString(), fileSize / 1024));

            Files.write(filePath, buffer);
            files.add(filePath);
        }

        return files;
    }

    private void deleteDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.printf("Failed to delete %s: %s%n", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.printf("Failed to clean up directory %s: %s%n", directory, e.getMessage());
        }
    }
}
