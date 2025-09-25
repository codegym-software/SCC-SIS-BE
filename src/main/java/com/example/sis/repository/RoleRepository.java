package com.example.sis.repository;

import com.example.sis.model.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsByRoleId(Integer roleId);
}
