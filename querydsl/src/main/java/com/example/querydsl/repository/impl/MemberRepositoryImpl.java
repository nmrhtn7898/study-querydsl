package com.example.querydsl.repository.impl;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.dto.QMemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.repository.MemberRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    @Autowired
    JPAQueryFactory queryFactory;

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            booleanBuilder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            booleanBuilder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            booleanBuilder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            booleanBuilder.and(member.age.loe(condition.getAgeLoe()));
        }

        QueryResults<MemberTeamDto> result = queryFactory
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = result.getResults();
        long total = result.getTotal();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            booleanBuilder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            booleanBuilder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            booleanBuilder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            booleanBuilder.and(member.age.loe(condition.getAgeLoe()));
        }

        List<MemberTeamDto> content = queryFactory
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Member> countQuery = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
//        return new PageImpl<>(content, pageable, count);
    }
}
