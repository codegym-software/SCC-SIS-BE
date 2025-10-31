// src/main/java/com/example/sis/keycloak/KeycloakAdminClient.java
package com.example.sis.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakAdminClient {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl; // VD: https://id.dev.codegym.vn/auth

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    private final RestTemplate rest = new RestTemplate();

    private String tokenEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);
    }
    private String adminUsersEndpoint() {
        return String.format("%s/admin/realms/%s/users", serverUrl, realm);
    }
    private String adminUserByIdEndpoint(String userId) {
        return String.format("%s/admin/realms/%s/users/%s", serverUrl, realm, userId);
    }

    /** Lấy admin access token qua client credentials */
    private String getAdminToken() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ResponseEntity<Map> rsp = rest.postForEntity(tokenEndpoint(), new HttpEntity<>(form, h), Map.class);
        if (!rsp.getStatusCode().is2xxSuccessful() || rsp.getBody() == null || !rsp.getBody().containsKey("access_token")) {
            throw new IllegalStateException("Không lấy được admin token từ Keycloak");
        }
        return (String) rsp.getBody().get("access_token");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getAdminToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /** Kiểm tra user tồn tại theo ID (sub) */
    public boolean existsById(String keycloakUserId) {
        try {
            ResponseEntity<Void> rsp = rest.exchange(
                    adminUserByIdEndpoint(keycloakUserId),
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Void.class
            );
            return rsp.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    /** Tìm userId theo email (exact=true); trả null nếu không thấy hoặc nhiều kết quả */
    @SuppressWarnings("unchecked")
    public String findUserIdByEmail(String email) {
        String url = adminUsersEndpoint() + "?email=" + email + "&exact=true";
        ResponseEntity<List> rsp = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders()), List.class);
        if (!rsp.getStatusCode().is2xxSuccessful() || rsp.getBody() == null || rsp.getBody().isEmpty()) return null;
        if (rsp.getBody().size() > 1) return null; // nếu realm cho phép trùng email -> xử theo policy
        Map<String, Object> user = (Map<String, Object>) rsp.getBody().get(0);
        return (String) user.get("id");
    }

    /** Tạo user; trả về userId mới (đọc từ header Location) */
    public String createUser(String username, String email, String firstName, String lastName, boolean enabled) {
        HttpHeaders h = authHeaders();
        Map<String, Object> body = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", enabled,
                "emailVerified", false
        );
        try {
            ResponseEntity<Void> rsp = rest.exchange(
                    adminUsersEndpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, h),
                    Void.class
            );
            if (rsp.getStatusCode() != HttpStatus.CREATED) {
                throw new IllegalStateException("Tạo user trên Keycloak thất bại, status=" + rsp.getStatusCode());
            }
            URI location = rsp.getHeaders().getLocation();
            if (location == null) {
                String id = findUserIdByEmail(email);
                if (id == null) throw new IllegalStateException("Không trích xuất được userId từ Location header");
                return id;
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException e) {
            // Xử lý lỗi 409 Conflict - User đã tồn tại trong Keycloak
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                String errorMessage = "Email \"" + email + "\" đã được sử dụng cho tài khoản đăng nhập trong hệ thống. Vui lòng sử dụng email khác.";
                try {
                    // Thử parse error message từ response body nếu có
                    if (e.getResponseBodyAsString() != null) {
                        String responseBody = e.getResponseBodyAsString();
                        if (responseBody.contains("username") || responseBody.contains("email")) {
                            // Giữ nguyên message từ Keycloak nhưng thêm email cụ thể
                            errorMessage = "Email \"" + email + "\" đã được sử dụng cho tài khoản đăng nhập trong hệ thống. Vui lòng sử dụng email khác.";
                        }
                    }
                } catch (Exception parseEx) {
                    // Nếu không parse được, dùng message mặc định
                }
                throw new IllegalArgumentException(errorMessage);
            }
            // Các lỗi khác từ Keycloak
            throw new IllegalStateException("Không thể tạo tài khoản đăng nhập: " + e.getMessage() + " (status: " + e.getStatusCode() + ")");
        }
    }

    /** Đảm bảo có user trên Keycloak & trả về userId (sub) */
    public String ensureUserAndGetId(String providedId, String email, String username, String fullName) {
        if (providedId != null && !providedId.isBlank()) {
            if (existsById(providedId)) return providedId;
            String[] names = splitName(fullName);
            return createUser(username, email, names[0], names[1], true);
        }
        String found = findUserIdByEmail(email);
        if (found != null) return found;
        String[] names = splitName(fullName);
        return createUser(username, email, names[0], names[1], true);
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return new String[]{parts[0], ""};
        String lastName = parts[parts.length - 1];
        String firstName = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
        return new String[]{firstName, lastName};
    }

    // import: org.springframework.http.*; java.util.Map;

    public void setTemporaryPassword(String userId, String password, boolean temporary) {
        HttpHeaders h = authHeaders();
        Map<String, Object> body = Map.of(
                "type", "password",
                "temporary", temporary,
                "value", password
        );
        rest.exchange(
                adminUserByIdEndpoint(userId) + "/reset-password",
                HttpMethod.PUT,
                new HttpEntity<>(body, h),
                Void.class
        );
    }

}
