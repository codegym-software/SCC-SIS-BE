package com.example.sis.repository;

import com.example.sis.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
