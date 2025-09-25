// src/main/java/com/example/sis/repository/RoleRepository.java
package com.example.sis.repository;

import com.example.sis.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> { }
