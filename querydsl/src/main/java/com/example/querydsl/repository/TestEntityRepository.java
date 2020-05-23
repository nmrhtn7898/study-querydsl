package com.example.querydsl.repository;

import com.example.querydsl.entity.TestEntity;
import com.example.querydsl.entity.TestId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEntityRepository extends JpaRepository<TestEntity, TestId> {
}
