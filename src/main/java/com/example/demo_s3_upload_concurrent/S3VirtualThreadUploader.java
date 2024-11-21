package com.example.demo_s3_upload_concurrent;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

public class S3VirtualThreadUploader {
    private final S3Client s3Client;
    private final String bucketName;
    private final Semaphore semaphore;

    public S3VirtualThreadUploader(AppConfig appConfig) {
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

    public void cleanUp() {
        if (this.s3Client != null) {
            this.s3Client.close();
        }
    }
}
