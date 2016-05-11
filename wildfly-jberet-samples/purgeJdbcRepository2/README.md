# Overview

This sample application demonstrates the use of `org.jberet.repository.PurgeBatchlet`,
 which removes unwanted job data from a job repository. The sample app is deployed to
 JBoss EAP or WildFly application server, which has been configured to use a jdbc batch
 job repository.
 
## Batch Job Definition 

This sample application contains 2 batch jobs:
 
 * `prepurge2.xml`: 
    * test job to be run first, and its job data (job executions and step
    executions) will then be removed.
    * contains a single batchlet-step implemented as inline-javascript.
 * `purgeJdbcRepository2.xml`: 
    * job to remove job data as specified with `PurgeBatchlet` configuration.
    * contains a single batchlet-step `PurgeBatchlet`.
  
## `org.jberet.repository.PurgeBatchlet` Configuration
 
 * `sql`: 
    * a string containing one or more sql statements, separated by ;
    * these sql statements are executed by `PurgeBatchlet` to remove unwanted job data.
 * `sqlFile`: 
    * the path to the resource file in the webapp WAR file.
    * contains one or more sql statements, separated by ;
    * these sql statements are executed by `PurgeBatchlet` to remove unwanted job data.
 * Other configuration options (see [org.jberet.repository.PurgeBatchlet](https://github.com/jberet/jsr352/blob/master/jberet-core/src/main/java/org/jberet/repository/PurgeBatchlet.java))
 
## Start and Configure JBoss EAP or WildFly

To start JBoss EAP or WildFly:

    cd $JBOSS_HOME/bin
    ./standalone.sh
    
To configure batch subsystem to use jdbc job repository, with CLI command:

    cd $JBOSS_HOME/bin
    ./jboss-cli.sh --connect
    
    # to view the current batch subsystem configuration:
    /subsystem=batch-jberet:read-resource(recursive=true)
    
    # to designate a jdbc job repository as the default job repository:
    /subsystem=batch-jberet/:write-attribute(name=default-job-repository, value=jdbc)

 
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
