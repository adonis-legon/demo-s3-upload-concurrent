package com.example.demo_s3_upload_concurrent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String awsProfile;

    private String awsS3BucketName;

    private String totalTemporalFiles;

    private String maxConcurrentUploads;

    private String httpMaxConnections;

    private String httpConnectionTimeout;

    private String httpConnectionAquisitionTimeout;

    public String getAwsProfile() {
        return this.awsProfile;
    }

    public void setAwsProfile(String awsProfile) {
        this.awsProfile = awsProfile;
    }

    public String getAwsS3BucketName() {
        return this.awsS3BucketName;
    }

    public void setAwsS3BucketName(String awsS3BucketName) {
        this.awsS3BucketName = awsS3BucketName;
    }

    public String getTotalTemporalFiles() {
        return this.totalTemporalFiles;
    }

    public void setTotalTemporalFiles(String totalTemporalFiles) {
        this.totalTemporalFiles = totalTemporalFiles;
    }

    public String getMaxConcurrentUploads() {
        return this.maxConcurrentUploads;
    }

    public void setMaxConcurrentUploads(String maxConcurrentUploads) {
        this.maxConcurrentUploads = maxConcurrentUploads;
    }

    public String getHttpMaxConnections() {
        return this.httpMaxConnections;
    }

    public void setHttpMaxConnections(String httpMaxConnections) {
        this.httpMaxConnections = httpMaxConnections;
    }

    public String getHttpConnectionTimeout() {
        return this.httpConnectionTimeout;
    }

    public void setHttpConnectionTimeout(String httpConnectionTimeout) {
        this.httpConnectionTimeout = httpConnectionTimeout;
    }

    public String getHttpConnectionAquisitionTimeout() {
        return this.httpConnectionAquisitionTimeout;
    }

    public void setHttpConnectionAquisitionTimeout(String httpConnectionAquisitionTimeout) {
        this.httpConnectionAquisitionTimeout = httpConnectionAquisitionTimeout;
    }

}