//package com.wan.batch.example;
//
//import com.wan.batch.example.member.MemberRepository;
//import org.springframework.batch.core.JobExecution;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.test.JobLauncherTestUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//@Configuration
//@EnableBatchProcessing
//public class TestJobConfig {
//
//    @Autowired
//    private JobLauncherTestUtils jobLauncherTestUtils;
//
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @Bean
//    public JobLauncherTestUtils jobLauncherTestUtils() throws Exception {
//
//        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
//
//        return new JobLauncherTestUtils();
//    }
//}
