package com.example.springboot_api.config.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true) // Bật method security để sử dụng @PreAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                // Swagger public
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**")
                .permitAll()

                // Auth public
                .requestMatchers("/auth/login", "/auth/register", "/auth/logout").permitAll()

                // WS public
                .requestMatchers("/ws/**", "/chat-test.html").permitAll()

                .requestMatchers("/uploads/**").permitAll()

                // Test AI Image generation (có thể remove sau)
                .requestMatchers("/user/ai-images/**").permitAll()
                .requestMatchers("/user/slides/**").permitAll()

                // ========== PHÂN QUYỀN ==========
                .requestMatchers("/admin/**").hasRole("ADMIN") // ROLE_ADMIN
                .requestMatchers("/lecturer/**").hasRole("TEACHER") // ROLE_TEACHER
                .requestMatchers("/api/exams/**").hasAnyRole("TEACHER", "STUDENT") // Exam system

                // Cả hai đều xài được
                .requestMatchers("/shared/**").hasAnyRole("ADMIN", "STUDENT")

                // Chỉ cần login
                .requestMatchers("/auth/me").authenticated()

                .anyRequest().authenticated());

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOriginPattern("*"); // CHÌA KHOÁ: cho mọi origin
        // Origin pattern không bao gồm path, chỉ domain
        // config.setAllowedOriginPatterns(List.of(
        // "http://localhost:*",
        // "https://localhost:*",
        // "http://*.ngrok-free.dev",
        // "https://*.ngrok-free.dev",
        // // Domain cụ thể nếu cần
        // "https://unshapen-splenetically-cheyenne.ngrok-free.dev"
        // ));

        config.setAllowCredentials(true); // BẮT BUỘC PHẢI TRUE nếu dùng cookie JWT

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
