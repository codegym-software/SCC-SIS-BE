package com.example.sis.repositories;

import com.example.sis.models.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface CenterRepository extends JpaRepository<Center, Integer> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    List<Center> findAllByOrderByNameAsc();
}
