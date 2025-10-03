package com.example.sis.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Cho phép preflight của mọi đường dẫn
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Auth profile
                                                .requestMatchers("/api/auth/profile").authenticated()

                                                // Users + User Views + Stats
                                                .requestMatchers(HttpMethod.GET, "/api/users").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/users").authenticated() // Quyền
                                                                                                                // SA đã
                                                                                                                // kiểm
                                                                                                                // tra
                                                                                                                // trong
                                                                                                                // service
                                                .requestMatchers(HttpMethod.GET, "/api/user-views").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/user-stats/**").authenticated()

                                                // Roles
                                                .requestMatchers(HttpMethod.GET, "/api/roles").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/roles/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/roles").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/roles/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/roles/**").authenticated()

                                                // Permissions
                                                .requestMatchers(HttpMethod.GET, "/api/permissions").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/permissions/**").authenticated()

                                                // Role-Permissions
                                                .requestMatchers(HttpMethod.GET, "/api/role-permissions/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/role-permissions/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/role-permissions/**")
                                                .authenticated()

                                                // Center Dropdown
                                                .requestMatchers(HttpMethod.GET, "/api/centers/lite").authenticated()

                                                // Centers
                                                .requestMatchers(HttpMethod.GET, "/api/centers").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/centers/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/centers/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/centers/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/centers/**").authenticated()

                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt()); // dùng JWT Bearer từ Keycloak

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(
                        @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOriginsProp) {

                List<String> allowedOrigins = Arrays.stream(allowedOriginsProp.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());

                CorsConfiguration config = new CorsConfiguration();
                // KHÔNG dùng "*" khi allowCredentials=true
                config.setAllowedOrigins(allowedOrigins);

                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                // Cho phép gửi Authorization + Content-Type…
                config.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
                // Nếu cần đọc Location/Link… từ FE
                config.setExposedHeaders(Arrays.asList("Location"));
                // Bearer token không cần cookie, nhưng set true cũng OK trong dev
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                // Áp cho toàn bộ API
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
