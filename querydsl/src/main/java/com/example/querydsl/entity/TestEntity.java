package com.example.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(TestId.class)
public class TestEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Id
    private Long testId;

    public TestEntity(Member member, Long testId) {
        this.member = member;
        this.testId = testId;
    }
}
