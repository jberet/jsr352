# Overview

 This module implements common CLI commands based on Vert.x for managing batch job data,
 including start/restart/stop/abandon a batch job execution, list jobs/job executions/
 /job instances/step executions, and view details of job executions and step executions. 
 
## How to Build jberet-vertx-shell
 
To clean and build:
 
 ``` 
 mvn clean install 
 ```

## How to Run CLI Commands in jberet-vertx-shell

### start Vert.x shell service, 
with `jberet-vertx-shell` and its dependencies in 
additional classpath.  The following is how I did this step in my machine:

```asciidoc
/Users/cfang/jberet/lib > 
~/vertx/bin/vertx run -conf '{"telnetOptions":{"port":5000}}' service:io.vertx.ext.shell -cp 
../bin
:jboss-batch-api_1.0_spec-1.0.0.Final.jar
:jberet-core-1.3.0.Beta6-SNAPSHOT.jar
:jberet-se-1.3.0.Beta6-SNAPSHOT.jar
:jboss-transaction-api_1.2_spec-1.0.0.Final.jar
:wildfly-security-manager-1.1.2.Final.jar
:jboss-logging-3.3.0.Final.jar
:jboss-marshalling-1.4.10.Final.jar
:jberet-vertx-shell-1.3.0.Beta6-SNAPSHOT.jar
:cdi-api-1.2.jar:weld-core-2.3.4.Final.jar
:weld-se-2.3.4.Final.jar
:weld-spi-2.3.SP2.jar
:jberet-support-1.3.0.Beta6-SNAPSHOT.jar
:validation-api-1.1.0.Final.jar
:/Users/cfang/dev/jsr352/test-apps/simple/target/simple-1.3.0.Beta6-SNAPSHOT.jar
:/Users/cfang/dev/jsr352/jberet-vertx/jberet-vertx-shell/target/jberet-vertx-shell-1.3.0.Beta6-SNAPSHOT.jar
:/Users/cfang/tmp/mysql-connector-java-5.1.36-bin.jar
```

Note:
* For easy viewing, I split the command, especially the long classpath into separate lines.
* The above command runs in `~/jberet/lib` directory, where all my JBeret jars reside, to avoid
  using paths for jar files in classpath.
* `../bin` is included in the classpath to make `bin/jberet.properties` available to Vert.x
  runtime to load JBeret configuration, including the use of JDBC (MySQL) job repository.
* `~/tmp/mysql-connector-java-5.1.36-bin.jar` is used by batch JDBC job repository.
  Not needed if `in-memory` batch job repository is used. Substitute with other JDBC driver 
  jars if other DBMS is used.
* `jsr352/test-apps/simple/target/simple-1.3.0.Beta6-SNAPSHOT.jar` is a sample app, so we have
  a batch job to test with.
* The shell service is started with telnet, for the sake of simplicity. SSH and HTTP can be 
  configured instead.
  
### start the CLI client

```asciidoc
telnet localhost 5000
Trying ::1...
Connected to localhost.
Escape character is '^]'.
__      __ ______  _____  _______  __   __
\ \    / /|  ____||  _  \|__   __| \ \ / /
 \ \  / / | |____ | :_) |   | |     \   /
  \ \/ /  |  ____||   __/   | |      > /
   \  /   | |____ | |\ \    | |     / //\
    \/    |______||_| \_\   |_| o  /_/ \_\

```

Run `help` command and you can see JBeret commands are there, along with built-in Vert.x commands:

```asciidoc
% help
available commands:
start-job
list-jobs
abandon-job-execution
count-job-instances
get-job-execution
get-step-execution
list-job-executions
list-job-instances
list-step-executions
restart-job-execution
stop-job-execution
metrics-info
metrics-ls
echo
sleep
help
cd
pwd
ls
net-ls
local-map-get
local-map-put
local-map-rm
bus-publish
bus-send
bus-tail
verticle-ls
verticle-deploy
verticle-undeploy
verticle-factories
exit
logout
jobs
fg
bg
```

Command auto-completion is supported, to save some typing. To get the usage for a command,
just run it with `--help`, or `-h` option:

```asciidoc
% list-job-executions -h
Usage: list-job-executions [-h] -j <value> [-r]

List job executions

Options and Arguments:
 -h,--help               this help
 -j,--job-name <value>   the name of the job
 -r,--running            whether to return only running job executions
```

To start a job:

```asciidoc
% start-job simple param1=value1,param2=value2
Started job: simple, job execution id: 11012
```

To list all job instances for job name `simple`:

```asciidoc
% list-job-instances --job-name simple
Job instances for job: simple
9127
9126
9125
9124
9123
```

To list all job executions for job name `simple`:

```asciidoc
% list-job-executions --job-name simple
Job executions for job simple:
11014                     COMPLETED
11013                     COMPLETED
11012                     ABANDONED
11011                     COMPLETED
11010                     ABANDONED
11009                     COMPLETED
11008                     ABANDONED
```

To get details for a specific job execution with id `11014`:

```asciidoc
% get-job-execution 11014
execution id              11014
job name                  simple
batch status              COMPLETED
exit status               COMPLETED
create time               Fri May 12 16:49:50 EDT 2017
start time                Fri May 12 16:49:50 EDT 2017
update time               Fri May 12 16:49:50 EDT 2017
end time                  Fri May 12 16:49:50 EDT 2017
job params                {param2=value2, param1=value1}
```

To list all step executions in a specific job execution with id `555`

```asciidoc
% list-step-executions --job-execution-id 555
Step executions in job execution 555:
805	COMPLETED
```

To get details for a step execution with id `14588` within job execution with id `11008`

```asciidoc
% get-step-execution 14588 -j 11008
execution id              14588
step name                 simple.step1
batch status              COMPLETED
exit status               COMPLETED
start time                Thu May 11 12:09:33 EDT 2017
end time                  Thu May 11 12:09:33 EDT 2017
READ_SKIP_COUNT           0
ROLLBACK_COUNT            0
READ_COUNT                15
WRITE_COUNT               15
PROCESS_SKIP_COUNT        0
COMMIT_COUNT              2
WRITE_SKIP_COUNT          0
FILTER_COUNT              0
persistent data           null
```

To restart a job execution with id `11012`

```asciidoc
% restart-job-execution 11012
javax.batch.operations.JobExecutionAlreadyCompleteException: 
JBERET000609: Job execution 11012 has already completed and cannot be restarted.
```

To abandon a job execution with id `11012`

```asciidoc
% abandon-job-execution 11012
Abandoned job execution: 11012
```

To list jobs:

```asciidoc
% list-jobs
Batch jobs: [simple]
```

To count the number of job instances belonging to a job name:

```asciidoc
% count-job-instances --job-name job1
350 job instances for job: job1
```

## Resources
* [JIRA Issue JBERET-329 Implement CLI/Shell for accessing batch job info](https://issues.jboss.org/browse/JBERET-329)