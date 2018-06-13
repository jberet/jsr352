# Overview

This test application verified REST API defined in `jberet-rest-api` module.
The test class uses JAX-RS client API to access a test web app
(`restAPI.war`) deployed to WildFly or JBoss EAP 7.

## Batch Jobs

This test application contains 3 batch jobs for testing REST API:

* `restJob1.xml`: contains 1 single batchlet-type step that prints the value 
                  of job parameter `jobParam1` and `jobParam2`;

* `restJob2.xml`: contains 1 single batchlet-type step that prints the value 
                  of job parameter `jobParam1` and `jobParam2`;

* `restJobWithParams`: contains 1 single batch-type step that can be configured to
                       fail or sleep, in order to better test job stopping and restarting:
    
    + when job parameter `fail` is set to true, the step execution and job execution
    will fail with `RuntimeException`;
    + when job parameter `sleepMillis` is set to positive long number, the step 
    execution and job execution will sleep for the specified milliseconds.

## Build and Run
 
To clean and build the application, deploy it to WildFly or JBoss EAP, and 
 run all tests:
 
 ``` 
 mvn clean install -Pwildfly
 ```
 
To undeploy the application from WildFly or JBoss EAP:
 
 ``` 
 mvn clean -Pwildfly
 ```
 
 or
 
 ```
 mvn wildfly:undeploy
 ```
 
## Include Job Data Front-end Module jberet-ui into WAR File
  
 First, you will need to build `jberet-ui` module with `--restUrl` for your
 `restAPI` application deployment:
 
 ```
 cd /Users/cfang/dev/jsr352/jberet-ui
 gulp --restUrl http://localhost:8080/restAPI/api/
 ```
 
 To incluee `jberet-ui` module into the WAR archive, run with `includeUI`
 maven profile. `jberet-ui` module adds a front-end UI for interacting with 
 batch job data.
 
 ```
 mvn clean install -PincludeUI
 ```
 
 To access `jberet-ui` web pages, go to http://localhost:8080/restAPI/
 