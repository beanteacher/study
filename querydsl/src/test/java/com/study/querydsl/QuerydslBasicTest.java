package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDTO;
import com.study.querydsl.dto.QMemberDTO;
import com.study.querydsl.dto.UserDTO;
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
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    @Autowired
    private JPAQueryFactory queryFactory;

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

        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultSet() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();
        results.getLimit();

        long fetchCount = queryFactory
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

        List<Member> result = queryFactory
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
        queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
    }
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory.select(member.count(),
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
      List<Tuple> result = queryFactory
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
        List<Member> memberList = queryFactory
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
        queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    /*
    * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    */
    @Test
    public void join_on_filtering() {
        List<Tuple> memberList = queryFactory.select(member, team)
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

        List<Tuple> result = queryFactory.select(member, team)
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

        Member findMember = queryFactory
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
        queryFactory.selectFrom(member)
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
        queryFactory.selectFrom(member)
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
        queryFactory.selectFrom(member)
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
        queryFactory
                .select(member.username,
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    void complexCase() {
        queryFactory.select(new CaseBuilder()
                .when(member.age.between(0,20)).then("0~20살")
                .when(member.age.between(21,30)).then("21~30살")
                .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    void constant() {
        queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
    }

    @Test
    void concat() {
        queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
    }
    
    @Test
    public void simpleProjection() {
        List<String> userNameList = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
    }

    @Test
    public void tupleProjection() {
        List<Tuple> userNameList = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
    }

    @Test
    public void findDTOByJPQL() {
        List<MemberDTO> memberList = em.createQuery("select " +
                "new com.study.querydsl.dto.MemberDTO(m.username, m.age) " +
                "from Member m"
                , MemberDTO.class)
                .getResultList();

        for(MemberDTO member : memberList) {
            System.out.println(member);
        }
    }

    @Test
    public void findDTOBySetter() {
        List<MemberDTO> result = queryFactory.select(Projections.bean(MemberDTO.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();
        for(MemberDTO memberDTO : result) {
            System.out.println(memberDTO);
        }
    }

    @Test
    public void findDTOByField() {
        List<MemberDTO> result = queryFactory.select(Projections.fields(MemberDTO.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();
        for(MemberDTO memberDTO : result) {
            System.out.println(memberDTO);
        }
    }

    @Test
    public void findDTOByConstructor() {
        List<MemberDTO> result = queryFactory.select(Projections.constructor(MemberDTO.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();
        for(MemberDTO memberDTO : result) {
            System.out.println(memberDTO);
        }
    }

    @Test
    public void findUserDTO() {
        QMember memberSub = new QMember("memberSub");
        List<UserDTO> result = queryFactory.select(Projections.fields(UserDTO.class
                        , member.username.as("name")
                        , ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();
        for(UserDTO memberDTO : result) {
            System.out.println(memberDTO);
        }
    }

    @Test
    public void findDtoByQueryProject() {
        queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String username, Integer age) {
        BooleanBuilder builder = new BooleanBuilder();
        if(username != null) {
            builder.and(member.username.eq(username));
        }

        if(age != null) {
            builder.and(member.age.eq(age));
        }

        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member2";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String username, Integer age) {

        return queryFactory.selectFrom(member)
                .where(usernameEq(username), ageEq(age))
                .fetch();
    }

    private Predicate ageEq(Integer age) {
        return age == null ? null : member.age.eq(age);
    }

    private Predicate usernameEq(String username) {
        return username == null ? null : member.username.eq(username);
    }

    @Test
    public void bulkUpdate() {
        // update 실행 전
        // member1 = 10 -> DB member1 영속성 컨테이너 member1
        // member2 = 20 -> DB member2 영속성 컨테이너 member2
        // member3 = 30 -> DB member3 영속성 컨테이너 member3
        // member4 = 40 -> DB member4 영속성 컨테이너 member4
        long count = queryFactory.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        /*
         * bulk update, delete를 실행하면 영속성 컨테이너의 데이터와
         * DB 데이터가 값이 달라지기 때문에 영속성 컨테이너를 비워준다.
         */
        em.flush();
        em.clear();

        // update 실행 후
        // member1 = 10 -> DB 비회원 영속성 컨테이너 member1
        // member2 = 20 -> DB 비회원 영속성 컨테이너 member2
        // member3 = 30 -> DB member3 영속성 컨테이너 member3
        // member4 = 40 -> DB member4 영속성 컨테이너 member4

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        // select 실행 후
        // member1 = 10 -> DB member1 영속성 컨테이너 member1
        // member2 = 20 -> DB member2 영속성 컨테이너 member2
        // member3 = 30 -> DB member3 영속성 컨테이너 member3
        // member4 = 40 -> DB member4 영속성 컨테이너 member4
    }

    @Test
    public void bulkDelete() {
        long count = queryFactory.delete(member)
                .where(member.age.lt(18))
                .execute();
    }
}