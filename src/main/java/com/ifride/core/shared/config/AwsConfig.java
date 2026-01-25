package com.ifride.core.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class AwsConfig {

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.SA_EAST_1)
                .build();
    }
}