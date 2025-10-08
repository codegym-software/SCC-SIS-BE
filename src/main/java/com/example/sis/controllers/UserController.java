// src/main/java/com/example/sis/controller/UserController.java
package com.example.sis.controllers;

import com.example.sis.dtos.user.CreateUserRequest;
import com.example.sis.dtos.user.UserResponse;
import com.example.sis.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    // CHỈ SUPER_ADMIN (trong DB) mới được tạo user
    @PostMapping
    @PreAuthorize("@authz.hasRole(authentication, 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    // SA: xem toàn hệ thống hoặc center bất kỳ
    // CENTER_MANAGER/ACADEMIC_STAFF: chỉ xem được trong center của mình
    @GetMapping
    @PreAuthorize("@authz.canListUsers(authentication, #centerId)")
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(value = "centerId", required = false) Integer centerId) {
        return ResponseEntity.ok(userService.getUsers(centerId));
    }
}
