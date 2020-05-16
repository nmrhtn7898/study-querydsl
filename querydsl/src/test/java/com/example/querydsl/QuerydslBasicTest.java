package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

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

    @Test
    public void aggregation() {
        List<Tuple> fetch = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = fetch.get(0);
        assertEquals(tuple.get(member.count()), 4);
        assertEquals(tuple.get(member.age.sum()), 100);
        assertEquals(tuple.get(member.age.avg()), 25);
        assertEquals(tuple.get(member.age.max()), 40);
        assertEquals(tuple.get(member.age.min()), 10);
    }

    @Test
    public void group() throws Exception {
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple tupleA = fetch.get(0);
        Tuple tupleB = fetch.get(1);
        assertEquals(tupleA.get(team.name), "teamA");
        assertEquals(tupleA.get(member.age.avg()), 15);

        assertEquals(tupleB.get(team.name), "teamB");
        assertEquals(tupleB.get(member.age.avg()), 35);
    }

    @Test
    public void join() throws Exception {
        List<Member> teamA = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(teamA)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();
        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());
        assertFalse(loaded, "패치 조인 미적용");
    }

    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();
        Member mem = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(mem.getTeam());
        assertTrue(loaded, "패치 조인 적용");
    }

    @Test
    public void subQuery() throws Exception {
        QMember member2 = new QMember("member2");
        Member result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(member2.age.max().as("maxAge"))
                                .from(member2)
                        )
                )
                .fetchOne();
        assertEquals(result.getAge(), 40);
    }

    @Test
    public void subQuery_goe_avg() throws Exception {
        QMember member2 = new QMember("member2");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(member2.age.avg().as("maxAge"))
                                .from(member2)
                        )
                )
                .fetch();
        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test
    public void selectSubquery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg().as("avg"))
                                .from(memberSub)
                                .where(memberSub.id.eq(member.id))
                )
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple.get(1, double.class));
        }
    }

}
