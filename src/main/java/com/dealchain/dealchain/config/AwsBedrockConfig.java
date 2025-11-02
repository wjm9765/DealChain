
package com.dealchain.dealchain.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class AwsBedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        // DefaultCredentialsProvider: 환경변수(.env를 spring-dotenv로 로드한 경우 포함),
        // 시스템 속성, ~/.aws/credentials 등을 순서대로 확인합니다.
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

        //한국 지역 지정
        Region region = Region.AP_NORTHEAST_2;

        return BedrockRuntimeClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
