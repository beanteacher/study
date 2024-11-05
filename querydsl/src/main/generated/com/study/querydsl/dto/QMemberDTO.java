package com.study.querydsl.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.study.querydsl.dto.QMemberDTO is a Querydsl Projection type for MemberDTO
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMemberDTO extends ConstructorExpression<MemberDTO> {

    private static final long serialVersionUID = -1034591153L;

    public QMemberDTO(com.querydsl.core.types.Expression<String> username, com.querydsl.core.types.Expression<Integer> age) {
        super(MemberDTO.class, new Class<?>[]{String.class, int.class}, username, age);
    }

}

