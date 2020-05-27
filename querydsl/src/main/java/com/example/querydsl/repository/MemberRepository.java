package com.example.querydsl.repository;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    Member findByUsername(String username);

}
