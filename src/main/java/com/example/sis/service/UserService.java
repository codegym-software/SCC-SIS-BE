// src/main/java/com/example/sis/service/UserService.java
package com.example.sis.service;

import com.example.sis.dto.user.CreateUserRequest;
import com.example.sis.dto.user.UserResponse;

import java.util.List;
public interface UserService {
    UserResponse createUser(CreateUserRequest req);
    List<UserResponse> getUsers(Integer centerId);
}
