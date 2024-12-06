package ca.gbc.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
        private final String[] nouthResourceUris = {

                "/swagger-ui",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resource/**",
                "/api-docs/**",
                "/aggregate/**",
                "/actuator/**"
        };
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)  throws Exception{



                log.info("Initializing Security Filter chain...");
                return httpSecurity
                        .csrf(AbstractHttpConfigurer::disable)// disable CSRF protection using the new syntax
                        //Authorize all HTTP requests, requiring authentication for any request


                        . authorizeHttpRequests(authorize-> authorize
                                .requestMatchers(nouthResourceUris)
                                .permitAll()
                                .anyRequest().authenticated())
                        // .authorizeHttpRequests(authorize -> authorize
                        //       .anyRequest().permitAll())          //allow all requests temporarily
                        //setup DAuth2 resource server to use JWT tokens for authentication
                        .oauth2ResourceServer(oauth2 -> oauth2
                                .jwt(Customizer.withDefaults()))
                        .build();
        }

}