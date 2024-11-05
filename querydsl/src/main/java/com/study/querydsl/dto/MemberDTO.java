package com.study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MemberDTO {
    private String username;
    private int age;

    @QueryProjection
    public MemberDTO(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
