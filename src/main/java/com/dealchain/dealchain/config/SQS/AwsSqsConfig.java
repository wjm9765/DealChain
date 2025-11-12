package com.dealchain.dealchain.config.SQS;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Getter
@Configuration
public class AwsSqsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${MESSAGE_COUNT}")
    private int count;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

        return SqsAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
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

        return SqsMessageListenerContainerFactory
                .builder()

                .sqsAsyncClient(sqsAsyncClient())
                .configure(options -> options
                        .maxMessagesPerPoll(count)
                )
                .build();
    }
}
