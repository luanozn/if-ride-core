package com.ifride.core.shared.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestClientConfig {
    @Bean
    TestRestTemplate rest() {
        return new TestRestTemplate();
    }
}