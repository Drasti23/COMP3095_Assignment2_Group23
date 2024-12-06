package ca.gbc.eventservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Failure rate threshold: 50%
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait for 30 seconds before allowing requests
                .slidingWindowSize(10) // Use last 10 calls to calculate failure rate
                .build();
        return CircuitBreaker.of("eventServiceCircuitBreaker", circuitBreakerConfig);
    }
}
