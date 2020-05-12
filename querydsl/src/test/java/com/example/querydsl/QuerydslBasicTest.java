package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    @Autowired
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void beforeEach() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamA);
        Member member4 = new Member("member4", 40, teamA);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void jpql() {
        Member singleResult = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertEquals(singleResult.getUsername(), "member1");
    }

    @Test
    public void querydsl() {
        Member mem = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertEquals(mem.getUsername(), "member1");
    }

    @Test
    public void search() {
        Member mem = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertEquals(mem.getUsername(), "member1");
    }

    @Test
    public void searchAndParam() {
        Member mem = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertEquals(mem.getUsername(), "member1");
    }

    @Test
    public void resultFetchTest() {
    List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

/*        queryFactory.selectFrom(member)
                .fetchOne();*/

        /*QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getResults();
        results.getTotal();*/

        queryFactory.selectFrom(member)
                .fetchCount();

    }

    @Test
    public void sort() {
        em.persist(new Member(null, 110));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        for (Member m : fetch) {
            System.out.println("username:" + m.getUsername() + " // age: " + m.getAge());
        }
    }

    @Test
    public void paging1() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertEquals(fetch.size(), 2);
    }



}
