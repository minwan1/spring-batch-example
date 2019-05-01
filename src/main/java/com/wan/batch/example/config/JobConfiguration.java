package com.wan.batch.example.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job(){
        return jobBuilderFactory.get("job")
                .start(step1(null))
                .next(step2(null))
                .build();
    }

    @Bean
    @JobScope
    public Step step1(@Value("#{jobParameters[date]}") String date){
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is step1");
                    log.info(">>>>> date = {}", date);
                    return RepeatStatus.FINISHED;
                }).build();
    }


    @Bean
    @JobScope
    public Step step2(@Value("#{jobParameters[date]}") String date) {
        return stepBuilderFactory.get("step2")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is step2");
                    log.info(">>>>> date = {}", date);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

}
