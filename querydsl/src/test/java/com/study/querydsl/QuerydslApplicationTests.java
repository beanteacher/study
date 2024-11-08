package com.study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.config.QueryDslConfig;
import com.study.querydsl.entity.Hello;
import com.study.querydsl.entity.QHello;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void test() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory jquery = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");

        Hello result = jquery
                .selectFrom(qHello)
                .fetchOne();

        assertThat(result).isEqualTo(hello);
        assertThat(result.getId()).isEqualTo(hello.getId());

    }

}
