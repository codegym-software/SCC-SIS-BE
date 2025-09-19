package com.example.sis.controller;

import com.example.sis.dto.UserRequest;
import com.example.sis.dto.UserResponse;
import com.example.sis.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody UserRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Page<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "userId,desc") String sort
    ) {
        Sort sortSpec;
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",", 2);
            String prop = parts[0].trim();
            String dir = parts[1].trim();
            sortSpec = Sort.by("asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC, prop);
        } else {
            // Mặc định: userId desc
            sortSpec = Sort.by(Sort.Direction.DESC, sort != null ? sort : "userId");
        }
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return service.list(pageable);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/mark-login")
    public UserResponse markLogin(@PathVariable Long id) {
        return service.markLogin(id);
    }
}
