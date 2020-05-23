package com.example.querydsl;

import com.example.querydsl.dto.MemberDto;
import com.example.querydsl.dto.QMemberDto;
import com.example.querydsl.dto.UserDto;
import com.example.querydsl.entity.*;
import com.example.querydsl.repository.TestEntityRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TestEntityRepository testEntityRepository;

    @BeforeEach
    public void beforeEach() {
/*        Team teamA = new Team("teamA");
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
        em.persist(member4);*/
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

    @Test
    public void basicCase() throws Exception {
        List<String> fetch = queryFactory
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }

    }

    @Test
    public void complexCase() throws Exception {
        List<String> fetch = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void constant() throws Exception {
        List<Tuple> a = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : a) {
            System.out.println(tuple);
        }

    }

    @Test
    public void concat() throws Exception {
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }

    }

    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple.get(member.username));
            System.out.println(tuple.get(member.age));
        }

    }

    @Test
    public void findDto() throws Exception {
        List<MemberDto> resultList = em.createQuery("select new com.example.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println(memberDto.getUsername());
            System.out.println(memberDto.getAge());
        }
    }

    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                        )
                )
                .from(member)
                .fetch();

        for (UserDto dto : fetch) {
            System.out.println(dto);
        }
    }

    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }

    }

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (usernameParam != null) {
            booleanBuilder.and(member.username.eq(usernameParam));
        }
        if (ageParam != null) {
            booleanBuilder.and(member.age.eq(ageParam));
        }
        return queryFactory
                .selectFrom(member)
                .where(booleanBuilder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam), ageEq(ageParam))
                .fetch();
    }

    private Predicate usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }

    private Predicate ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    @Test
    public void bulkUpdate() throws Exception {
        /*Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.id.eq(1L))
                .fetchOne();*/
        /*Member member = em.createQuery("select m from Member m where m.id = :id", Member.class)
                .setParameter("id", 1L)
                .getSingleResult();

        System.out.println(member);


        Member member2 = em.createQuery("select m from Member m where m.id = :id", Member.class)
                .setParameter("id", 1L)
                .getSingleResult();

        System.out.println(member2);*/

        Member member1 = memberRepository.findByUsername("member1");

        Member member11 = memberRepository.findByUsername("member1");
        Member member12 = memberRepository.findByUsername("member1");
        Member member13 = memberRepository.findByUsername("member1");

        assertEquals(member1, member11);
        assertEquals(member11, member12);
        assertEquals(member12, member13);
        assertEquals(member1, member13);
    }

    @Test
    public void bulkAdd() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() throws Exception {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

    }

    @Test
    public void sqlFunction() throws Exception {
        List<String> fetch = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "m"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void sqlFunction2() throws Exception {
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("lower({0})", member.username
                        )))
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void compositeKeySelectTest() throws Exception {
        Member member1 = new Member("member1", 10, null);
        em.persist(member1);

        TestEntity testEntity = new TestEntity(member1, 2L);
        em.persist(testEntity);

        em.flush();
        em.clear();

        TestId testId = new TestId();
        testId.setMember(member1.getId());
        testId.setTestId(2L);
//        TestEntity findTestEntity = em.find(TestEntity.class, testId);                  /*entitymanager.find(*/
/*        TestEntity findTestEntity = em                                                  // jpql find
                .createQuery("select t from TestEntity t where t.member.id = :memberId and t.testId = :testId", TestEntity.class)
                .setParameter("memberId", member1.getId())
                .setParameter("testId", 2L)
                .getSingleResult();*/
        TestEntity findTestEntity = queryFactory                                         //querydsl find
                .selectFrom(QTestEntity.testEntity)
                .where(QTestEntity.testEntity.member.id.eq(member1.getId()), QTestEntity.testEntity.testId.eq(2L))
                .fetchOne();

//        TestEntity findTestEntity = testEntityRepository.findById(testId).orElse(null); // spring data jpa


        assertEquals(testEntity.getMember().getId(), findTestEntity.getMember().getId());
        assertEquals(testEntity.getTestId(), findTestEntity.getTestId());
    }


}
