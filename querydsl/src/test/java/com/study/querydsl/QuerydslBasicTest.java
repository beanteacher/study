package com.study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.study.querydsl.entity.QMember.member;
import static com.study.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void before() {
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
    public void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m from Member m " +
                        "where m.username=:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl() {
        JPAQueryFactory factory = new JPAQueryFactory(em);

        Member findMember = factory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultSet() {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        List<Member> fetch = factory
                .selectFrom(member)
                .fetch();

        Member fetchOne = factory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = factory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = factory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();
        results.getLimit();

        long fetchCount = factory
                .selectFrom(member)
                .fetchCount();
    }

    /* 회원 정렬 순서
    * 1. 회원 나이 내림차순
    * 2. 회원 이름 오름차순
    * 단 2. 에서 회원 이름이 없으면 마지막에 출력 (nulls last)*/
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        jpaQueryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
    }
    @Test
    public void aggregation() {
        List<Tuple> result = jpaQueryFactory.select(member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
    }

    /*
    * 팀의 이름과 각 팀의 평균 연령
    * */
    @Test
    public void group() throws Exception {
      List<Tuple> result = jpaQueryFactory
              .select(team.name, member.age.avg())
              .from(member)
              .join(member.team, team)
              .groupBy(team.name)
              .fetch();

      Tuple teamA = result.get(0);
      Tuple teamB = result.get(1);
    }

    @Test
    public void join() {
        List<Member> memberList = jpaQueryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(memberList)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /* 세타 조인
    * 회원의 이름이 팀의 이름과 같은 회원 조회
    * */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        jpaQueryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    /*
    * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    */
    @Test
    public void join_on_filtering() {
        List<Tuple> memberList = jpaQueryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple : memberList) {
            System.out.println(tuple);
        }
    }

    /*
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = jpaQueryFactory.select(member, team)
                .from(member)
                // 기존의 조인 : (leftJoin member.team, team)
                // 연관관계가 없는 조인 : (leftJoin team)
                // 값의 일치 여부로만 비교
                .leftJoin(team).on(member.username.eq(team.name))
                .where(member.username.eq(team.name))
                .fetch();
    }

    @Test
    public void fetchJoin() {
        em.flush();
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
    }

    /*
    * 나이가 가장 많은 회원 조회
    * */
    @Test
    void subQuery() {
        QMember subMember = new QMember("memberSub");
        jpaQueryFactory.selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(subMember.age.max())
                                .from(subMember)
                )).fetch();

    }

    /*
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    void subQueryGoe() {
        QMember subMember = new QMember("memberSub");
        jpaQueryFactory.selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(subMember.age.avg())
                                .from(subMember)
                )).fetch();

    }

    /*
     * 나이가 10살 위로 속하는 회원 조회
     * */
    @Test
    void subQueryIn() {
        QMember subMember = new QMember("memberSub");
        jpaQueryFactory.selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(subMember.age)
                                .from(subMember)
                                .where(subMember.age.gt(10))
                )).fetch();

    }

    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        jpaQueryFactory
                .select(member.username,
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
    }

    @Test
    public void basicCase() {
        List<String> result = jpaQueryFactory
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    void complexCase() {
        jpaQueryFactory.select(new CaseBuilder()
                .when(member.age.between(0,20)).then("0~20살")
                .when(member.age.between(21,30)).then("21~30살")
                .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    void constant() {
        jpaQueryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
    }

    @Test
    void concat() {
        jpaQueryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
    }
}
