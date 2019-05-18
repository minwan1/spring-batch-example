package com.wan.batch.example.config;


import com.wan.batch.example.member.Member;
import com.wan.batch.example.member.MemberRepository;
import com.wan.batch.example.member.ProcessStatus;
import com.wan.batch.example.member.QueueItemReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final MemberRepository memberRepository;
    private final int CHUNK_SIZE = 10;

    @Bean
    public Job completeUserJob(JobBuilderFactory jobBuilderFactory, Step completeJobStep) {
        return jobBuilderFactory.get("completeUserJob")
                .incrementer(new RunIdIncrementer())
                .start(completeJobStep)
                .build();
    }
    @Bean
    public Step completeJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("completeUserStep")
                .allowStartIfComplete(true)
                .<Member, Member> chunk(CHUNK_SIZE)
                .reader(completeMemberReader())
                .processor(completeUserProcessor())
                .writer(completeUserWriter())
                .build();
    }

    @Bean
    @StepScope
    public QueueItemReader<Member> completeMemberReader() {
        List<Member> oldMembers =
                memberRepository.findByCreatedAtBeforeAndProcessStatusEquals(
                        LocalDateTime.now().minusDays(1), ProcessStatus.WAITING);
        return new QueueItemReader<>(oldMembers);  // 전체를 다읽어옴
    }

    public ItemProcessor<Member, Member> completeUserProcessor() {
        return new ItemProcessor<Member, Member>() {
            @Override
            public Member process(Member member) {
                return member.setCompleted();
            }

        };
    }

    public ItemWriter<Member> completeUserWriter() {

        return new ItemWriter<Member>() {
            @Override
            public void write(List<? extends Member> users) {
                // 위에 청크가 있어서 10개씩 커밋함.
                memberRepository.saveAll(users);
            }
        };
    }

}
