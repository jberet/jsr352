# Overview

This is a sample webapp that demonstrates batch job scheduling using EJB Timer.
The WAR package includes [jberet-schedule-executor module](https://github.com/jberet/jsr352/tree/master/jberet-schedule/jberet-schedule-executor)
and [jberet-schedule-timer](https://github.com/jberet/jsr352/tree/master/jberet-schedule/jberet-schedule-timer).

## How to Build

    mvn clean install

## How to Run Tests

1. Start JBoss EAP 7 or WildFly server:
    
    ```
    $JBOSS_HOME/bin/standalone.sh
    ```
  
2. Run [tests](https://github.com/jberet/jsr352/blob/master/wildfly-jberet-samples/scheduleTimer/src/test/java/org/jberet/samples/wildfly/schedule/timer/ScheduleTimerIT.java)
    with `wildfly` profile:
    
    ```
    mvn -Pwildfly install
    ```
     
3. Undeploy application:
    
    ```
    mvn wildfly:undeploy
    ```
    
## How to Run with `jberet-ui`

 First, you will need to build `jberet-ui` module with `--restUrl` for this application deployment:
 
 ```
 cd <project-home>/jberet-ui
 gulp --restUrl http://localhost:8080/scheduleTimer/api/
 ```
 
 To incluee `jberet-ui` module into the WAR archive, run with `includeUI`
 maven profile. `jberet-ui` module adds a front-end UI for interacting with 
 batch job data. The following command will clean, build, deploy, and run the tests:
 
 ```
 mvn clean install -PincludeUI
 ```
 
 To access `jberet-ui` web pages, go to http://localhost:8080/scheduleTimer/, where you can 
 view, manage, or schedule job executions.
 
## Screenshot:

![timerExecutor sample screenshot](https://cloud.githubusercontent.com/assets/2079251/14944318/6edf9558-0fbd-11e6-95f2-efaf97e8706a.png)
