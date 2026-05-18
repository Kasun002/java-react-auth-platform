package com.org.auth.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

/**
 * AWS client beans for SQS and SES.
 *
 * <p>
 * When {@code cloud.aws.endpoint} is set (LocalStack for local dev), both
 * clients
 * are pointed at that endpoint. In production, leave the property blank and the
 * SDK resolves the correct regional endpoint automatically.
 * </p>
 */
@Configuration
public class AwsConfig {

    @Value("${cloud.aws.region.static:us-east-1}")
    private String region;

    @Value("${cloud.aws.credentials.access-key:test}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:test}")
    private String secretKey;

    /** Empty string means no override — uses the real AWS endpoint. */
    @Value("${cloud.aws.endpoint:}")
    private String endpointOverride;

    @Bean
    public SqsClient sqsClient() {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }
        return builder.build();
    }

    @Bean
    public SesClient sesClient() {
        SesClientBuilder builder = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }
        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
    }
}
