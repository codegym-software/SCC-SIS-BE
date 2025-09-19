package com.example.sis.service.impl;

import com.example.sis.dto.UserRequest;
import com.example.sis.dto.UserResponse;
import com.example.sis.exception.BadRequestException;
import com.example.sis.exception.NotFoundException;
import com.example.sis.model.User;
import com.example.sis.repository.UserRepository;
import com.example.sis.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public UserResponse create(UserRequest req) {
        if (repo.existsByEmail(req.getEmail().toLowerCase())) {
            throw new BadRequestException("Email đã tồn tại");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Mật khẩu không được để trống khi tạo user");
        }

        User u = new User();
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail().toLowerCase());
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        return toRes(repo.save(u));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User u = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user id=" + id));
        return toRes(u);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(this::toRes);
    }

    @Override
    public UserResponse update(Long id, UserRequest req) {
        User u = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user id=" + id));

        String newEmail = req.getEmail().toLowerCase();
        if (!u.getEmail().equalsIgnoreCase(newEmail) && repo.existsByEmail(newEmail)) {
            throw new BadRequestException("Email đã tồn tại");
        }

        u.setFullName(req.getFullName());
        u.setEmail(newEmail);

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPasswordHash(encoder.encode(req.getPassword()));
        }

        if (req.getIsActive() != null) {
            u.setIsActive(req.getIsActive());
        }

        return toRes(repo.save(u));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy user id=" + id);
        }
        repo.deleteById(id);
    }

    @Override
    public UserResponse markLogin(Long id) {
        User u = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user id=" + id));
        u.setLastLoginAt(LocalDateTime.now());
        return toRes(repo.save(u));
    }

    private UserResponse toRes(User u) {
        UserResponse r = new UserResponse();
        r.setUserId(u.getUserId());
        r.setFullName(u.getFullName());
        r.setEmail(u.getEmail());
        r.setIsActive(u.getIsActive());
        r.setLastLoginAt(u.getLastLoginAt());
        r.setCreatedAt(u.getCreatedAt());
        r.setUpdatedAt(u.getUpdatedAt());
        return r;
    }
}
