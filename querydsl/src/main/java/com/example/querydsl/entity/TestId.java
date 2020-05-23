package com.example.querydsl.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(of = {"member", "testId"})
public class TestId implements Serializable {

    private Long member;

    private Long testId;

}
