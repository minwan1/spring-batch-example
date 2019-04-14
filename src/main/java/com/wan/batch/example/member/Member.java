//package com.wan.batch.example.member;
//
//
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import javax.persistence.*;
//
//@Table
//@Entity
//@Getter
//@NoArgsConstructor(access = AccessLevel.PRIVATE)
//public class Member {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private String id;
//
//    @Column(name="name", nullable = false)
//    private String name;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name="level", nullable = false)
//    private Level level;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name="login_count", nullable = false)
//    private long loginCount;
//
//    @Column(name="recommend_count" ,nullable = false)
//    private long recommendCount;
//
//
//    public void upgradeLevel() {
//        final Level nextLevel = this.level.nextLevel();
//
//        if (nextLevel == null) throw new IllegalStateException(this.level + "은 업그레이드가 불가능합니다.");
//
//        this.level = nextLevel;
//    }
//
//
//}
