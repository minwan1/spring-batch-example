package com.wan.batch.example.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByCreatedAtBeforeAndProcessStatusEquals(LocalDateTime localDateTime, ProcessStatus processStatus);

}
