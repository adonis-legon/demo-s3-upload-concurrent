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

    public List<String> uploadFilesFromDirectory(String directory) {
        Path tempDir = null;
        S3FileManager fileManager = new S3FileManager(this.appConfig);
        List<String> uploadedFileNames = new ArrayList<>();

        try {
            // Create temporary directory for files
            String tempDirPrefix = directory + "-temp-";
            tempDir = Files.createTempDirectory(tempDirPrefix);

            // Generate random files
            List<Path> filesToUpload = generateRandomFilesWithVaryingSizes(tempDir,
                    Integer.parseInt(this.appConfig.getTotalTemporalFiles()));

            long startTime = System.currentTimeMillis();
            fileManager.uploadFilesConcurrently(filesToUpload);
            long endTime = System.currentTimeMillis();

            // Collect the file names (keys) that were uploaded to S3
            uploadedFileNames = filesToUpload.stream()
                    .map(path -> path.getFileName().toString())
                    .toList();

            System.out.printf("Uploaded %d files in %d ms%n", filesToUpload.size(), (endTime - startTime));
        } catch (Exception e) {
            System.err.println("Error during batch upload: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileManager.cleanUp();
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }

        return uploadedFileNames;
    }

    public void downloadFilesFromS3(List<String> s3Keys, String downloadDirectoryName) {
        S3FileManager fileManager = new S3FileManager(this.appConfig);

        try {
            // Create download directory
            Path downloadDir = Path.of(downloadDirectoryName);
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir);
            }

            long startTime = System.currentTimeMillis();
            fileManager.downloadFilesConcurrently(s3Keys, downloadDir);
            long endTime = System.currentTimeMillis();

            System.out.printf("Downloaded %d files in %d ms%n", s3Keys.size(), (endTime - startTime));
        } catch (Exception e) {
            System.err.println("Error during batch download: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileManager.cleanUp();
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
