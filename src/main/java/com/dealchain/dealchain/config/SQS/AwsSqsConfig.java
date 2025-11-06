package com.dealchain.dealchain.config.SQS;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Getter
@Configuration
public class AwsSqsConfig {

    @Value("${aws.credentials.access-key}")
    private String awsAccessKey;

    @Value("${aws.credentials.secret-key}")
    private String awsSecretKey;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${MESSAGE_COUNT}")
    private int count;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .credentialsProvider(() -> new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return awsAccessKey;
                    }

                    @Override
                    public String secretAccessKey() {
                        return awsSecretKey;
                    }
                })
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory() {
        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient())
                .build();
    }

    @Bean(name = "batchSqsListenerContainerFactory")
    public SqsMessageListenerContainerFactory<Object> batchSqsListenerContainerFactory() {
        // acknowledgementMode 설정을 제거하여 프레임워크 기본(AUTO 동작을 기대)으로 맡김
        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient())
                .configure(options -> options
                        .maxMessagesPerPoll(count)
                )
                .build();
    }
}
