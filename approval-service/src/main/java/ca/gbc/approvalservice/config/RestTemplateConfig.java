package ca.gbc.approvalservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;


@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CircuitBreaker eventServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)  // 50% failure rate
                .waitDurationInOpenState(java.time.Duration.ofMillis(10000)) // 10 seconds
                .slidingWindowSize(10)  // Sliding window size of 10 calls
                .build();

        return CircuitBreaker.of("eventService", config);
    }

}



