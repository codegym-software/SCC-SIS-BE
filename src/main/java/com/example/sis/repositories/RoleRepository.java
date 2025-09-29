package com.example.sis.repositories;

import com.example.sis.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    List<Role> findAllByOrderByNameAsc();
    List<Role> findByActiveTrueOrderByNameAsc();
}
