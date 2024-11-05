package com.study.querydsl.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class MemberSearchCondition {

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
