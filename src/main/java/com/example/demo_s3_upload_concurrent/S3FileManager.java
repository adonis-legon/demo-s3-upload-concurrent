package com.example.demo_s3_upload_concurrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3FileManager {
    private final S3Client s3Client;
    private final String bucketName;
    private final Semaphore semaphore;

    public S3FileManager(AppConfig appConfig) {
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder()
                .profileName(appConfig.getAwsProfile())
                .build();

        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
                .maxConnections(Integer.parseInt(appConfig.getHttpMaxConnections()))
                .connectionTimeout(Duration.ofSeconds(Long.parseLong(appConfig.getHttpConnectionTimeout())))
                .connectionAcquisitionTimeout(
                        Duration.ofSeconds(Long.parseLong(appConfig.getHttpConnectionAquisitionTimeout())));

        this.s3Client = S3Client.builder().credentialsProvider(credentialsProvider)
                .httpClient(httpClientBuilder.build()).build();
        this.bucketName = appConfig.getAwsS3BucketName();
        this.semaphore = new Semaphore(Integer.parseInt(appConfig.getMaxConcurrentUploads()));
    }

    public void uploadFilesConcurrently(List<Path> filePaths) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> uploadFutures = filePaths.stream()
                    .map(filePath -> CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                uploadFile(filePath);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Upload interrupted", e);
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
        }
    }

    private void uploadFile(Path filePath) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath.getFileName().toString())
                    .build();

            long startTime = System.currentTimeMillis();
            s3Client.putObject(request, RequestBody.fromFile(filePath));
            long endTime = System.currentTimeMillis();
            System.out.printf("Thread-%d (Available Permits: %d): Successfully uploaded %s to bucket %s in %d ms%n",
                    Thread.currentThread().threadId(),
                    semaphore.availablePermits(),
                    filePath.getFileName(),
                    bucketName,
                    (endTime - startTime));
        } catch (Exception e) {
            System.err.printf("Error uploading file %s: %s%n", filePath.getFileName(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void downloadFilesConcurrently(List<String> s3Keys, Path downloadDirectory) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Create download directory if it doesn't exist
            if (!Files.exists(downloadDirectory)) {
                Files.createDirectories(downloadDirectory);
            }

            List<CompletableFuture<Void>> downloadFutures = s3Keys.stream()
                    .map(s3Key -> CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                downloadFile(s3Key, downloadDirectory);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Download interrupted", e);
                        } catch (IOException e) {
                            throw new RuntimeException("Download failed", e);
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(downloadFutures.toArray(new CompletableFuture[0])).join();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create download directory", e);
        }
    }

    private void downloadFile(String s3Key, Path downloadDirectory) throws IOException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            long startTime = System.currentTimeMillis();

            Path downloadPath = downloadDirectory.resolve(s3Key);

            try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(request)) {
                Files.copy(s3Object, downloadPath);
            }

            long endTime = System.currentTimeMillis();
            System.out.printf("Thread-%d (Available Permits: %d): Successfully downloaded %s from bucket %s in %d ms%n",
                    Thread.currentThread().threadId(),
                    semaphore.availablePermits(),
                    s3Key,
                    bucketName,
                    (endTime - startTime));
        } catch (Exception e) {
            System.err.printf("Error downloading file %s: %s%n", s3Key, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void cleanUp() {
        if (this.s3Client != null) {
            this.s3Client.close();
        }
    }
}
