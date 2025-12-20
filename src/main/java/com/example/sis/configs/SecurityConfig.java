package com.example.sis.configs;
import com.example.sis.configs.DefaultRoleAutoAssignFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Autowired
        private DefaultRoleAutoAssignFilter defaultRoleAutoAssignFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .headers(headers -> headers
                                                .httpStrictTransportSecurity(hsts -> hsts.disable()))
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Cho phép preflight của mọi đường dẫn
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                
                                                // Actuator health cho Docker healthcheck
                                                .requestMatchers("/actuator/health").permitAll()

                                                // Auth profile
                                                .requestMatchers("/api/auth/profile").authenticated()
                                                .requestMatchers("/api/users/profile").authenticated()

                                                // Users + User Views + Stats
                                                .requestMatchers(HttpMethod.GET, "/api/users").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/users").authenticated() // Quyền
                                                                                                                // SA đã
                                                                                                                // kiểm
                                                                                                                // tra
                                                                                                                // trong
                                                                                                                // service
                                                .requestMatchers(HttpMethod.GET, "/api/user-views").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/user-views/**").authenticated()
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

                                                // Programs
                                                .requestMatchers(HttpMethod.GET, "/api/programs").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/programs/lite").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/programs").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/programs/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/programs/**").authenticated()

                                                // Modules (Học phần)
                                                .requestMatchers(HttpMethod.GET, "/api/modules").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/modules/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/modules").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/modules/**").authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/api/modules/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/modules/**").authenticated()

                                                // File Upload
                                                .requestMatchers(HttpMethod.POST, "/api/files/upload/**").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll() // Cho phép truy cập file đã upload

                                                // Classes
                                                .requestMatchers(HttpMethod.GET, "/api/classes").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/classes/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/classes").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/classes/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/classes/**").authenticated()

                                                // Enrollments (students in class)
                                                .requestMatchers(HttpMethod.GET, "/api/classes/*/students/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/classes/*/students")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/api/classes/*/students/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/classes/*/students/**")
                                                .authenticated()

                                                // Class-Lecturer assignments
                                                .requestMatchers(HttpMethod.GET, "/api/classes/*/lecturers")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/classes/*/lecturers/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/classes/*/lecturers/*")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/classes/*/lecturers/*")
                                                .authenticated()

                                                // Student Classes
                                                .requestMatchers(HttpMethod.GET, "/api/students/my-classes")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/students/*/classes")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/lecturers/*/classes")
                                                .authenticated()

                                                // User-Role assignments
                                                .requestMatchers(HttpMethod.GET, "/api/user-roles/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/user-roles/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/user-roles/**")
                                                .authenticated()

                                                // Students (Hồ sơ học viên)
                                                .requestMatchers(HttpMethod.POST, "/api/students").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/students").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/students/search").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/students/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/students/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/students/**").authenticated()

                                                // Attendance
                                                .requestMatchers(HttpMethod.GET, "/api/attendance-schedules").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/classes/*/attendance-sessions").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/classes/*/attendance/statistics").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/classes/*/attendance/export/excel").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/attendance-sessions/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/attendance-sessions").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/attendance-sessions/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/attendance-sessions/**").authenticated()

                                                // Grade Entries (Nhập điểm)
                                                .requestMatchers(HttpMethod.GET, "/api/grade-entries").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/grade-entries/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/grade-entries").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/grade-entries/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/grade-entries").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/grade-entries").authenticated()

                                                // Notifications
                                                .requestMatchers(HttpMethod.GET, "/api/notifications").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/notifications/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/notifications").authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/api/notifications/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/notifications/**").authenticated()

                                                // Chat (AI Chatbot with RESTful API)
                                                .requestMatchers(HttpMethod.POST, "/api/chat/sessions").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/chat/sessions").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/chat/sessions/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/chat/sessions/*/title").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/chat/sessions/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/chat/sessions/*/messages").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/chat/sessions/*/messages/stream").authenticated()

                                                // Admin Knowledge Base (ADMIN only)
                                                .requestMatchers(HttpMethod.POST, "/api/admin/knowledge/**").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/admin/knowledge/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/admin/knowledge/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/admin/knowledge/**").authenticated()

                                                // Admin Chat Analytics (ADMIN only)
                                                .requestMatchers(HttpMethod.GET, "/api/admin/chat-analytics/**").authenticated()

                                                // Lessons & Learning Progress (Bài học & Tiến trình học tập)
                                                .requestMatchers(HttpMethod.GET, "/api/lessons/module/*/progress").authenticated() // Get module progress
                                                .requestMatchers(HttpMethod.GET, "/api/lessons/module/*").authenticated() // Get lessons by module
                                                .requestMatchers(HttpMethod.POST, "/api/lessons/*/progress").authenticated() // Update progress (STUDENT only)
                                                .requestMatchers(HttpMethod.POST, "/api/lessons").authenticated() // Create lesson (ADMIN only)
                                                .requestMatchers(HttpMethod.GET, "/api/lessons/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/lessons/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/lessons/**").authenticated()

                                                // Quizzes (Bộ câu hỏi trắc nghiệm)
                                                .requestMatchers(HttpMethod.POST, "/api/quizzes").authenticated() // Create quiz (ADMIN only)
                                                .requestMatchers(HttpMethod.POST, "/api/quizzes/*/questions/import").authenticated() // Import questions (ADMIN only)
                                                .requestMatchers(HttpMethod.GET, "/api/quizzes/lesson/*").authenticated() // Get quiz detail
                                                .requestMatchers(HttpMethod.GET, "/api/quizzes/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/quizzes/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**").authenticated()

                                                // Quiz Attempts (Làm bài quiz)
                                                .requestMatchers(HttpMethod.POST, "/api/quizzes/attempts/start").authenticated() // Start attempt (STUDENT only)
                                                .requestMatchers(HttpMethod.POST, "/api/quizzes/attempts/*/answers").authenticated() // Submit answer (STUDENT only)
                                                .requestMatchers(HttpMethod.POST, "/api/quizzes/attempts/*/submit").authenticated() // Submit quiz (STUDENT only)
                                                .requestMatchers(HttpMethod.GET, "/api/quizzes/attempts/quiz/*/my-attempts").authenticated() // Get attempt history (STUDENT only)

                                                // WebSocket endpoint
                                                .requestMatchers("/ws/**").permitAll()

                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))); // Custom converter to extract Keycloak roles

                // Đăng ký filter sau BearerTokenAuthenticationFilter
                http.addFilterAfter(defaultRoleAutoAssignFilter, BearerTokenAuthenticationFilter.class);

                return http.build();
        }

        /**
         * Custom JWT Authentication Converter to extract roles from Keycloak token.
         * Keycloak stores roles in: realm_access.roles (realm roles) and resource_access.{client}.roles (client roles)
         * This converter adds "ROLE_" prefix to all extracted roles for Spring Security.
         */
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                
                // Custom authorities converter to extract Keycloak roles
                converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                        Collection<GrantedAuthority> authorities = new ArrayList<>();
                        
                        // Extract realm roles from realm_access.roles
                        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                        if (realmAccess != null && realmAccess.containsKey("roles")) {
                                @SuppressWarnings("unchecked")
                                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                                for (String role : realmRoles) {
                                        // Add ROLE_ prefix for Spring Security
                                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                                }
                        }
                        
                        // Extract client roles from resource_access.{client}.roles
                        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
                        if (resourceAccess != null) {
                                for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> clientAccess = (Map<String, Object>) entry.getValue();
                                        if (clientAccess != null && clientAccess.containsKey("roles")) {
                                                @SuppressWarnings("unchecked")
                                                List<String> clientRoles = (List<String>) clientAccess.get("roles");
                                                for (String role : clientRoles) {
                                                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                                                }
                                        }
                                }
                        }
                        
                        return authorities;
                });
                
                return converter;
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