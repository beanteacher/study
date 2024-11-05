package com.study.querydsl.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.study.querydsl.dto.QMemberTeamDTO is a Querydsl Projection type for MemberTeamDTO
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMemberTeamDTO extends ConstructorExpression<MemberTeamDTO> {

    private static final long serialVersionUID = -2114783054L;

    public QMemberTeamDTO(com.querydsl.core.types.Expression<Long> memberId, com.querydsl.core.types.Expression<String> username, com.querydsl.core.types.Expression<Integer> age, com.querydsl.core.types.Expression<Long> teamId, com.querydsl.core.types.Expression<String> teamName) {
        super(MemberTeamDTO.class, new Class<?>[]{long.class, String.class, int.class, long.class, String.class}, memberId, username, age, teamId, teamName);
    }

}

