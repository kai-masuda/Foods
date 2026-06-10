package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Unit;


public interface UnitRepository extends JpaRepository<Unit, Long>{

    Optional<Unit> findByUnitName(String unitName);
}
