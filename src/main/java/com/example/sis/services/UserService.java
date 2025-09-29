// src/main/java/com/example/sis/service/UserService.java
package com.example.sis.services;

import com.example.sis.dtos.user.CreateUserRequest;
import com.example.sis.dtos.user.UserResponse;

import java.util.List;
public interface UserService {
    UserResponse createUser(CreateUserRequest req);
    List<UserResponse> getUsers(Integer centerId);
}
