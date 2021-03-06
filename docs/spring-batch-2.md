# 간단한잡 만들기
<!-- vscode-markdown-toc -->
* 1. [개발 환경 구성](#)
* 2. [배치 메타 테이블 정보](#-1)
	* 2.1. [Job Instance (BATCH_JOB_INSTANCE)](#JobInstanceBATCH_JOB_INSTANCE)
	* 2.2. [Job Execution (BATCH_JOB_EXECUTION)](#JobExecutionBATCH_JOB_EXECUTION)
	* 2.3. [Job parameteres (BATCH_JOB_EXECUTION_PARAMS)](#JobparameteresBATCH_JOB_EXECUTION_PARAMS)
	* 2.4. [Step](#Step)
	* 2.5. [StepExection (BATCH_STEP_EXECUTION)](#StepExectionBATCH_STEP_EXECUTION)
* 3. [간단한 잡 만들기 1](#1)
* 4. [간단한 잡 만들기 2](#2)
	* 4.1. [BATCH_JOB_EXECUTION](#BATCH_JOB_EXECUTION)
	* 4.2. [BATCH_JOB_EXECUTION_PARAMS](#BATCH_JOB_EXECUTION_PARAMS)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->




##  1. <a name=''></a>개발 환경 구성
먼저 간단한 잡을 만들기 위해 Spring batch 의존성 주입을 해줘야 합니다. 먼저 배치 의존성과 배치 테스트를 하기 위한 배치 테스트를 의존성 주입을 합니다.

```java
dependencies {
    ...
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    testImplementation 'org.springframework.batch:spring-batch-test'
}
```

먼저 개발할 수 있는 환경을 위해 데이터베이스 구성이 필요합니다. docker-compose를 통해 데이터베이스를 구성해보겠습니다. 프로젝트 Root 폴더에 다음과 같이 docker-compose.yaml 파일 구성해야 합니다. (물론 docker가 설치돼있으셔야 합니다.)
* [예제](https://github.com/minwan1/spring-batch-example/tree/feature/batch1-job)

```yml
version: '3'

services:
  mysql:
     container_name: batch.mysql
     image: mysql/mysql-server:5.7
     environment:
      MYSQL_ROOT_HOST: '%'
      MYSQL_DATABASE: "batch"
      MYSQL_ALLOW_EMPTY_PASSWORD: "yes"
     ports:
     - "3306:3306"
     volumes:
     - ./volumes/mysql:/var/lib/mysql
```
* [도커 윈도우 설치](https://docs.docker.com/docker-for-windows/install/)
* [도커 맥 설치](https://docs.docker.com/docker-for-mac/install/)

docker-compose를 구성하셨다면 docker-compose를 아래와 같은 명령어로 실행시키시면 local 환경에서 mysql 구성이 되고 batch라는 데이터베이스가 생성됩니다. 그리고 local에 데이터베이스가 설치된것처럼 사용 가능하게 됩니다.

```
$ docker-compose up -d
```


##  2. <a name='-1'></a>배치 메타 테이블 정보
간단한 잡을 만들어보기 전에 실제로 스프링 배치를 돌렸을 때 어떻게 아래의 메타 테이블에 데이터들과 객체들이 저장되는지 간단히 알아보겠습니다.

![](https://i.imgur.com/IBxzbj5.png)


###  2.1. <a name='JobInstanceBATCH_JOB_INSTANCE'></a>Job Instance (BATCH_JOB_INSTANCE)
JobInstance는 배치에서 Job이 실행될 때 하나의 실행 단위입니다. 그리고 이 JobInstantce는 JobExecution와 1:N의 관계 롤 가집니다. 그리고 이 JobExecution은 JobInstance 성공, 실패, 이미 실행됐는지 등에 정보 등을 저장하고 있습니다.

###  2.2. <a name='JobExecutionBATCH_JOB_EXECUTION'></a>Job Execution (BATCH_JOB_EXECUTION)
JobExecution은 JobInstance에 대한 실행 정보를 나타냅니다. Job exectuion은 Job Instance를 실행했던 성공 실패 여부 왜 실패했는지에 대한 정보를 갖고 있는 클래스라고 보시면 됩니다. 실제 데이터베이스에도 성공, 실패에대한 정보들이 저장됩니다. 아래클래스는 BATCH_JOB_EXECUTION에 도메인이라고 생각하시면 될것같습니다.

```java
public class JobExecution extends Entity {  
    private final JobParameters jobParameters;    
    private JobInstance jobInstance;    
    private volatile Collection<StepExecution> stepExecutions = Collections.synchronizedSet(new LinkedHashSet<>()); 
    private volatile BatchStatus status = BatchStatus.STARTING;  private volatile Date startTime = null;   
    private volatile Date createTime = new Date(System.currentTimeMillis());   
    private volatile Date endTime = null;  
    private volatile Date lastUpdated = null; 
    private volatile ExitStatus exitStatus = ExitStatus.UNKNOWN; 
    private volatile ExecutionContext executionContext = new ExecutionContext(); 
    private transient volatile List<Throwable> failureExceptions = new CopyOnWriteArrayList<>(); 
    private final String jobConfigurationName;
```
위에는 JobExecution class입니다. JobExecution은 다음과 같은 정보를 가지고 있습니다.

* jobParameteres : Job 실행에 필요한 파라미터 정보입니다.
* jobInstance : Job 실행의 단위가 되는 객체입니다.
* stepExecutions : StepExecution을 여러 개 가질 수 있는 Collection 타입입니다.
* status : Job의 실행 상태를 나타내는 필드입니다. 
    * COMPLETED, STARTING, STARTED, STOPPING, STOPPED, FAILED, ABANDONED, UNKNOWN (DEFAULT : STARTING)
* startTime : Job이 실행된 시간입니다. null이면 시작하지 않았다는것을 나타냅니다.
* createTime : JobExecution이 생성된 시간입니다.
* endTime : JobExecution이 끝난 시간입니다.
* lastUpdated : 마지막으로 수정된 시간입니다.
* exitStatus : Job 실행 결과에 대한 상태를 나타냅니다. 
    * UNKNOWN, EXECUTING, COMPLETED, NOOP, FAILED, STOPPED(DEFAULT : UNKNOWN)
* executionContext : Job 실행 사이에 유지해야하는 사용자 데이터가 들어 있습니다.
* failureExceptions : Job 실행 중 발생한 예외를 List타입으로 저장합니다.
* jobConfigurationName : Job 설정 이름을 나타냅니다.


###  2.3. <a name='JobparameteresBATCH_JOB_EXECUTION_PARAMS'></a>Job parameteres (BATCH_JOB_EXECUTION_PARAMS)
JobParameteres는 Job이 실행될 때 필요한 파라미터들을 Map타입으로 저장하는 객체입니다. JobParameteres는 JobInstance를 구분하는 기준이 됩니다. 예를들어 하나의 Job을 실행할때 JobParameter로 date정보를 넘기면 하나의 JobInstance가 생성이됩니다. 하지만 동일한 JobParameter로 date를 넘긴다면 JobInstance는 생성되지 않을것이고 실행되지 않을것입니다. 즉 JobInstance와 JobParameters는 1:1 관계입니다. 이것은 JobParameters를 넘기지 않아도 JobInstance는 한번만 생성됩니다.

###  2.4. <a name='Step'></a>Step
Step은 배치처리를 정의하고 제어하는 데 필요한 모든 정보가 들어있는 도메인 객체입니다. Job을 처리하는 실질적인 단위로 쓰입니다. 모든 Job에는 1개 이상의 Step이 있어야 합니다.

###  2.5. <a name='StepExectionBATCH_STEP_EXECUTION'></a>StepExection (BATCH_STEP_EXECUTION)
Job에 JobExecution이라는 Job 실행 정보가 있다면 Step에는 StepExecution이라는 Step 실행 정보를 담는 객체가 있습니다. 각각의 Step이 실행될 때마다 StepExecution이 생성됩니다. 다음은 StepExecution 클래스입니다.


```java
public class StepExecution extends Entity {
    private final JobExecution jobExecution;
    private final String stepName;
    private volatile BatchStatus status = BatchStatus.STARTING;
    private volatile int readCount = 0;
    private volatile int writeCount = 0;
    private volatile int commitCount = 0;
    private volatile int rollbackCount = 0;
    private volatile int readSkipCount = 0;
    private volatile int processSkipCount = 0;
    private volatile int writeSkipCount = 0;
    private volatile Date startTime = new Date(System.currentTimeMillis());
    private volatile Date endTime = null;
    private volatile Date lastUpdated = null;
    private volatile ExecutionContext executionContext = new ExecutionContext();
    private volatile ExitStatus exitStatus = ExitStatus.EXECUTING;
    private volatile boolean terminateOnly;
    private volatile int filterCount;
    private transient volatile List<Throwable> failureExceptions = new CopyOnWriteArrayList<Throwable>();
```
* jobExecution : 현재의 JobExecution 정보를 담고 있는 필드입니다.
* stepName : Step의 이름을 가지고 있는 필드 입니다.
* status : Step의 실행 상태를 나타내는 필드입니다.
    * COMPLETED, STARTING, STARTED, STOPPING, STOPPED, FAILED, ABANDONED, UNKNOWN (DEFAULT : STARTING)
* readCount : 성공적으로 읽은 레코드 수입니다.
* writeCount : 성공적으로 쓴 레코드 수입니다.
* commitCount : Step의 실행에 대해 커밋된 트랜잭션 수입니다.
* rollbackCount : Step의 실행에 대해 롤백된 트랜잭션 수입니다.
* processSkipCount : 프로세스가 실패해 건너뛴 레코드 수 입니다.
* writeSkipCount : 쓰기에 실패해 건너뛴 레코드 수 입니다.
* startTime : Step이 실행된 시간입니다. null이면 시작하지 않았다는것을 나타냅니다.
* endTime : Step의 실행 성공 여부와 관련 없이 Step이 끝난 시간입니다.
* lastUpdated : 마지막으로 수정된 시간 입니다.
* executionContext : Step 실행 사이에 유지해야하는 사용자 데이터가 들어 있습니다.
* exitStatus : Step 실행 결과에 대한 상태를 나타냅니다.
    * UNKNOWN, EXECUTING, COMPLETED, NOOP, FAILED, STOPPED (DEFAULT : UNKNOWN)
* terminateOnly : Job 실행 중지 여부입니다.
* filterCount : 실행에서 필터링된 레코드 수입니다.
* failureExceptions : Step 실행 중 발상핸 예외를 List 타입으로 저장합니다.



##  3. <a name='1'></a>간단한 잡 만들기 1
그럼 실제로 위에 이론을 바탕으로 간단한 잡을 하나 등록해보고 실행 시켜보겠습니다. 잡은 하나이상에 스텝을 가지기 때문에 스텝을 빈으로 만들어서 실행할 job에 등록하셔야합니다. 아래와같이 잡을 만들어 빈으로 등록할 수 있습니다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class jobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job(){
        return jobBuilderFactory.get("job")
                .start(step1())
                .build();
    }

    @Bean
    public Step step1(){
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>> This is Step1");
                    return RepeatStatus.FINISHED;
                }).build();
    }

}
```
이것을 실행 시키면 다음과 같은 화면을 보게될것입니다.
![](https://i.imgur.com/AoFuBrr.png)

그런 다음 BATCH_JOB_INSTANCE 테이블을 조회하면 다음과 같이 Job정보가 테이블에 들어가 있는것을 볼 수 있습니다. Job Parameter를 넘기지 않았으므로 BATCH_JOB_EXECUTION_PARAMS에는 아무런 정보도 존재하지 않습니다.

JOB_INSTANCE_ID | VERSION | JOB_NAME | JOB_KEY|
------- | ------- | ------- | -------|
1 | 0 | job | d41d8cd98f00b204e9800998ecf8427e|

* JOB_INSTANCE_ID
    * BATCH_JOB_INSTANCE 테이블의 PK
* JOB_NAME
    * 수행한 Batch Job Name


실제 아주 간단한 잡을 등록해보고 실행해봤습니다. 앞에서 배운 이론과 같이 잡이 실행되고 배치 메타 데이터베이스에 잡의 실행 정보가 저장되는 것까지 확인을 했습니다.

만약 위에 Job을 다시 실행한다면 다음과 이미 스텝이 완료되었다고 하면서 Job이 실행되지 않습니다.
![](https://i.imgur.com/Tt0rOUZ.png)
그리고 BATCH_JOB_EXECUTION에는 다음과 같이 Job실행정보가 추가 되었을것입니다. 

JOB_EXECUTION_ID | VERSION | JOB_INSTANCE_ID | CREATE_TIME | START_TIME | END_TIME | STATUS | EXIT_CODE | EXIT_MESSAGE | LAST_UPDATED | JOB_CONFIGURATION_LOCATION
-----------------|---------|-----------------|-------------|------------|----------|--------|-----------|--------------|--------------|---------------------------
1 | 2 | 1 | 2019-05-01 05:00:07 | 2019-05-01 05:00:07 | 2019-05-01 05:00:07 | COMPLETED | COMPLETED |  | 2019-05-01 05:00:07 | NULL
2 | 2 | 1 | 2019-05-01 05:00:37 | 2019-05-01 05:00:37 | 2019-05-01 05:00:38 | COMPLETED | NOOP | All steps already completed or no steps configured for this job. | 2019-05-01 05:00:38 | NULL

BATCH_JOB_INSTANCE 테이블에는 따로 정보가 추가되지 않았습니다. Spring batch은 따로 Job parameter가 변경되지 않으면 해당 Job을 이전 Job과 같은 Job으로 인지합니다. 그렇기 때문에 이전 Job과 같은 Job으로 인지하고 위와같은 상황이 발생한것입니다. 위에 문제를 해결하기위해서는 다른 JobParameter를 등록해서 실행해야합니다. 아래 예제에서는 실제 JobParameter를 넘기는 예제를 만들어보겠습니다.

##  4. <a name='2'></a>간단한 잡 만들기 2
BATCH_JOB_INSTANCE 테이블은 Job Parameter에 따라 생성되는 테이블이다. 이것은 Spring Batch가 실행될 때 외부에서 받을 수 있는 파라미터를 의미한다. 예를 들어, 특정 날짜 JobParameter로 넘기면 Spring Batch에서는 해당 날짜 데이터로 조회/ 가공/ 입력 등의 작업을 할 수 있다. 같은 Batch Job이라도 Job Parameter가 다르면 BATCH_JOB_INSTANCE에는 기록되며, Job parameter가 없거나 같다면 기록되지 않습니다. 그럼 실제 Job parameter를 어떤 식으로 사용하는지 또 어떻게 관리되는지 알아보겠습니다.

* Jobparameter를 사용할 때 Step에 jobScope 어노테이션을 사용하지 않는다면 exception이 발생합니다.

먼저 위에 잡만들기 1을 아래와같이 변경해줘야합니다.
```java

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class jobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return jobBuilderFactory.get("job")
                .start(step1(null))
                .build();
    }

    @Bean
    @JobScope
    public Step step1(@Value("#{jobParameters[date]}") String date) {
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step1");
                    log.info(">>>>> date = {}", date);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}

```
[예제](https://github.com/minwan1/spring-batch-example/tree/feature/batch2-job-parameter)
* @JobScope는 Step 선언문에서 사용 가능합니다.
    * JobScope는 하나의 Job동안에 빈의 scope를 갖는다.
* @StepScope는 Tasklet이나 ItemReader, ItemWriter, ItemProcessor에서 사용할 수 있습니다.
    * StepScope는 하나의 Job동안 호출되는 만큼의 빈의 scope를 가지게 됩니다




위에 Job을 실행하기 위해서 아래와 같이 job parameter를 넘길 수 있습니다.
![](https://i.imgur.com/PvQI2pl.png)


이렇게 실행하면 아래와 같이 BATCH_JOB_INSTANCE에는 기존과 같은 Job에 이름이지만 새로운 Job 인스턴스가 추가된 것을 볼 수 있습니다. 또 Job 또한 실행되는 것을 확인할 수 있습니다.


JOB_INSTANCE_ID | VERSION | JOB_NAME | JOB_KEY|
------- | ------- | ------- | -------|
1 | 0 | job | d41d8cd98f00b204e9800998ecf8427e
2 | 0 | job | 83eda6e86aa43ff7becb7968d1ebb945

그런데 그대로 파라미터를 같은 값으로 실행하면 아래와 같이 에러 메시지를 만나며 Job 실행되지 않습니다. 이 문제를 해결하려면 파라미터 값을 변경해서 실행해야 합니다.

```java
Caused by: org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException: A job instance already exists and is complete for parameters={date=20190421}.  If you want to run this job again, change the parameters.
```

그럼 이번에 기존에 넘긴 파라미터에서 아래와 같이 값을 변경해서 앱을 실행해보겠습니다.

```java
{date=20190422}
```

이렇게 배치를 실행하면 아래와 같이 새로운 잡인스턴스가 잘 추가된것을 볼 수 있습니다.

JOB_INSTANCE_ID | VERSION | JOB_NAME | JOB_KEY
----------------|---------|----------|--------
1 | 0 | job | d41d8cd98f00b204e9800998ecf8427e
2 | 0 | job | 83eda6e86aa43ff7becb7968d1ebb945
3 | 0 | job | 7d58a461dae8e2a4524ad8ff10a313c5


즉, 위에서 말한 대로 동일한 Job이 Job Parameter가 달라지면 그때마다 BATCH_JOB_INSTANCE에 생성되며, 동일한 Job Parameter는 여러 개 존재할 수 없습니다.


###  4.1. <a name='BATCH_JOB_EXECUTION'></a>BATCH_JOB_EXECUTION

다음으로 볼것은 BATCH_JOB_EXECUTION 테이블입니다.

JOB_EXECUTION_ID | VERSION | JOB_INSTANCE_ID | CREATE_TIME | START_TIME | END_TIME | STATUS | EXIT_CODE | EXIT_MESSAGE | LAST_UPDATED | JOB_CONFIGURATION_LOCATION
------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | --------
1 | 2 | 1 | 2019-04-21 16:21:59 | 2019-04-21 16:21:59 | 2019-04-21 16:21:59 | COMPLETED | COMPLETED |  | 2019-04-21 16:21:59 | NULL|
2 | 2 | 2 | 2019-04-21 17:12:49 | 2019-04-21 17:12:49 | 2019-04-21 17:12:49 | COMPLETED | COMPLETED |  | 2019-04-21 17:12:49 | NULL|
3 | 2 | 3 | 2019-04-21 17:21:13 | 2019-04-21 17:21:13 | 2019-04-21 17:21:13 | COMPLETED | COMPLETED |  | 2019-04-21 17:21:13 | NULL|

JOB_EXECUTION 테이블을 보시면 3개의 ROW가 존재하는 것을 확인할 수 있습니다. 앞서 실행했던 파라미터가 없는 job, date=20190421, date=20190422 파라미터로 실행했던 job까지 이렇게 3개의 실행 데이터입니다.(만약 Job Parameter가 없는 상태에서 2번 실행했다면 지금 초 3개의 EXECUTION을 가지고 있습니다.)

JOB_EXECUTION와 JOB_INSTANCE는 부모-자식 관계이다. JOB_EXECUTION은 자신의 부모 JOB_INSTANCE가 성공, 실패했던 모든 내역을 갖고 있습니다다. 실제 그런지 예제를 돌려보겠습니다.


아래 예제는 배치를 수행하는 과정에서 Exception이 발생한 상황입니다. 실행을 해도 Exception을 던지기 때문에 BatchJob 인스턴스는 실패할 것입니다.
```java
 @Bean
    public Job job() {
        return jobBuilderFactory.get("job")
                .start(step1(null))
                .next(step2(null))
                .build();
    }

    @Bean
    @JobScope
    public Step step1(@Value("#{jobParameters[date]}") String date) {
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    throw new IllegalArgumentException("step1에서 실패합니다.");
                })
                .build();
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
```

실패를 하면 아래와 같이 상태에 실패와 함께 왜 실패했는지 BATCH_JOB_EXECUTION에 로그가 남습니다.


JOB_EXECUTION_ID | VERSION | JOB_INSTANCE_ID | CREATE_TIME | START_TIME | END_TIME | STATUS | EXIT_CODE | EXIT_MESSAGE | LAST_UPDATED | JOB_CONFIGURATION_LOCATION|
------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | --------
1 | 2 | 1 | 2019-04-21 16:21:59 | 2019-04-21 16:21:59 | 2019-04-21 16:21:59 | COMPLETED | COMPLETED |  | 2019-04-21 16:21:59 | NULL
2 | 2 | 2 | 2019-04-21 17:12:49 | 2019-04-21 17:12:49 | 2019-04-21 17:12:49 | COMPLETED | COMPLETED |  | 2019-04-21 17:12:49 | NULL
3 | 2 | 3 | 2019-04-21 17:21:13 | 2019-04-21 17:21:13 | 2019-04-21 17:21:13 | COMPLETED | COMPLETED |  | 2019-04-21 17:21:13 | NULL
4 | 2 | 4 | 2019-01-12 09:47:24 | 2019-01-12 09:47:24 | 2019-01-12 09:47:24 | FAILED | FAILED | java.lang.IllegalArgumentException: step1?? ?????.↵⇥at com.example.batch.job.jobConfiguration.lambda$step1$0(jobConfiguration.java:35)↵⇥at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction<br>... | 2019-01-12 09:47:24 | NULL|


그럼 이번에는 이 step1을 Exception이 아닌 정상적으로 다시 실행해보겠습니다.

```java
    @Bean
    @JobScope
    public Step step1(@Value("#{jobParameters[date]}") String date) {
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step1");
                    log.info(">>>>> date = {}", date);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

```

그럼 BATCH_JOB_EXECUTION 테이블은 아래와 같이 정상적으로 처리가 될 것입니다.

JOB_EXECUTION_ID | VERSION | JOB_INSTANCE_ID | CREATE_TIME | START_TIME | END_TIME | STATUS | EXIT_CODE | EXIT_MESSAGE | LAST_UPDATED | JOB_CONFIGURATION_LOCATION
------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | ------- | --------
1 | 2 | 1 | 2019-04-21 16:21:59 | 2019-04-21 16:21:59 | 2019-04-21 16:21:59 | COMPLETED | COMPLETED |  | 2019-04-21 16:21:59 | NULL
2 | 2 | 2 | 2019-04-21 17:12:49 | 2019-04-21 17:12:49 | 2019-04-21 17:12:49 | COMPLETED | COMPLETED |  | 2019-04-21 17:12:49 | NULL
3 | 2 | 3 | 2019-04-21 17:21:13 | 2019-04-21 17:21:13 | 2019-04-21 17:21:13 | COMPLETED | COMPLETED |  | 2019-04-21 17:21:13 | NULL
4 | 2 | 4 | 2019-01-12 09:47:24 | 2019-01-12 09:47:24 | 2019-01-12 09:47:24 | FAILED | FAILED | java.lang.IllegalArgumentException: step1?? ?????.↵⇥at com.example.batch.job.jobConfiguration.<br>...| 2019-01-12 09:47:24 | NULL
5 | 2 | 4 | 2019-01-12 09:59:41 | 2019-01-12 09:59:41 | 2019-01-12 09:59:41 | COMPLETED | COMPLETED |  | 2019-01-12 09:59:41 | NULL

여기에서 보면 JOB_EXECUTION_ID 4, 5를 봐보면 JOB_INTANCE_ID가 4인 것을 볼 수 있습니다. 그리고 실패와 성공의 정보가 담겨있는 것을 알 수 있습니다. 여기에서 실패한 Job instance를 동일한 JobParameter 실행했음에도 job이 실행되어 성공한 것을 볼 수 있습니다. 이렇듯 Spring Batch에서 실패한 Job에 한에서는 같은 Job parameter를 넘기더라도 실행을 할 수 있습니다.



###  4.2. <a name='BATCH_JOB_EXECUTION_PARAMS'></a>BATCH_JOB_EXECUTION_PARAMS
BATCH_JOB_EXECUTION_PARAM테이블은 BATCH_JOB_EXECUTION 테이블이 생성될 당시에 입력받은 Job Parameter를 담고 있습니다.

|JOB_EXECUTION_ID | TYPE_CD | KEY_NAME | STRING_VAL | DATE_VAL | LONG_VAL | DOUBLE_VAL | IDENTIFYING|
------- | ------- | ------- | ------- | ------- | ------- | ------- | -------
2 | STRING | date | 20190421 | 1970-01-01 00:00:00 | 0 | 0 | Y
3 | STRING | date | 20190422 | 1970-01-01 00:00:00 | 0 | 0 | Y


# 마무리하며
이번 장에서는 간단한 배치 잡을 만들어보고 실행해보았습니다. 그리고 배치를 실행하면서 배치 메타 테이블에 어떠한 정보들이 저장되는지 확인해보았습니다. 다음 장에서는 간단한 배치 잡이 아닌 고도화된 배치 잡을 실행해보고 Spring batch 메타 테이블 어떠한 정보들이 저장되는지 알아보겠습니다.
