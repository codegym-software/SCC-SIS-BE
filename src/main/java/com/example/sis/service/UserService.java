package com.example.sis.service;

import com.example.sis.dto.UserRequest;
import com.example.sis.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse create(UserRequest req);
    UserResponse getById(Long id);
    Page<UserResponse> list(Pageable pageable);
    UserResponse update(Long id, UserRequest req);
    void delete(Long id);
    UserResponse markLogin(Long id);
}
