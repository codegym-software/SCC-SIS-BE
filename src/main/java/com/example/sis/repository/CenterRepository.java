package com.example.sis.repository;

import com.example.sis.model.Center;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterRepository extends JpaRepository<Center, Integer> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
