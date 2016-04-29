# Overview

 This module provides support for scheduling batch job executions in batch applications.
 It contains 2 sub-modules that provides 3 types of batch job scheduler:
 
 * jberet-schedule-executor sub-module
    * scheduler based on `java.util.concurrent.ScheduledExecutorService`, suitable for Java SE applications.
    * scheduler based on `javax.enterprise.concurrent.ManagedScheduledExecutorService`, suitable for Java EE applications.
 * jberet-schedule-timer sub-module
    * scheduler based on EJB Timer
  
 Other types of job scheduler implementations are also possible by extending `org.jberet.schedule.JobScheduler`.
 
 
## How to Build jberet-schedule
 
To clean and build:
 
 ``` 
 mvn clean install 
 ```

## How to Use jberet-schedule

* In Java SE applications:
    * include jberet-schedule-executor module in application classpath.
    * See [tests](https://github.com/jberet/jsr352/blob/master/jberet-schedule/jberet-schedule-executor/src/test/java/org/jberet/schedule/ExecutorSchedulerIT.java) using jberet-schedule-executor in Java SE environment.
* In Java EE or web applications deployed to JBoss EAP & WildFly:
    * include jberet-schedule-executor module and optionally jberet-schedule-timer module in application package.
    * Sample app using jberet-schedule-executor: [wildfly-jberet-samples/scheduleExecutor/pom.xml](https://github.com/jberet/jsr352/blob/master/wildfly-jberet-samples/scheduleExecutor/pom.xml)
    * Sample app using jberet-schedule-timer: [wildfly-jberet-samples/scheduleTimer/pom.xml](https://github.com/jberet/jsr352/blob/master/wildfly-jberet-samples/scheduleTimer/pom.xml)

## Resources
* [JIRA Issue JBERET-222 Support Batch Job Scheduling](https://issues.jboss.org/browse/JBERET-222)
* [JBeret Forum Thread on Batch Job Scheduling with JBeret](https://developer.jboss.org/thread/269527)