package com.example.querydsl.repository;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Tuple;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTestEntity {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() throws Exception {
        // given
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);
        // when
        Member findMember = memberJpaRepository.findById(member.getId()).get();
        List<Member> result = memberJpaRepository.findAll();
        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        // then
        assertEquals(member, findMember);
        assertThat(result).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void basicQuerydslTest() throws Exception {
        // given
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);
        // when
        Member findMember = memberJpaRepository.findById(member.getId()).get();
        List<Member> result = memberJpaRepository.findAll_Querydsl();
        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1");
        // then
        assertEquals(member, findMember);
        assertThat(result).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void searchTest() throws Exception {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);
//        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertThat(result).extracting("username").containsExactly("member4");

    }


}