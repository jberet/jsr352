## Overview

This module supports integration between JBeret and Apache Camel, by
 providing a Camel JBeret component, and reusable batch artifacts for
 interacting with Apache Camel.
 
 
### Camel JBeret Component

Camel JBeret component producer supports the following types of operations and
their URL format:

* `jberet:jobs`
  lists job names. The response type is `java.util.Set<String>`.
  
* `jberet:jobs/job1`
  starts the job job1. any job parameters should be passed as message body as 
  `java.util.Properties`. The response type is `long` (job execution id).
  
* `jberet:jobs/job1/start`
  starts the job `job1`, same as above. any job parameters should be passed as
   message body as `java.util.Properties`. The response type is `long` (job execution id).
  
* `jberet:jobinstances?jobName=job1&start=0&count=10`
  lists job instances, `jobName` query param is required, `start` and `count`
   query params are both optional, and defaults to `0` and `10`, respectively.
    The response type is `java.util.List<JobInstance>`.
  
* `jberet:jobinstances/count?jobName=job1`
  counts job instances of the job `job1`. `jobName` query param is required.
   The response type is `int`.
  
* `jberet:jobexecutions/running?jobName=job1`
  lists all running job executions of the job `job1`. `jobName` query param
   is usually specified, but if omitted, it defaults to `*` and lists 
   running job executions of all jobs currently known to the batch runtime. 
   The response type is `java.util.List<Long>` (job execution ids).
  
* `jberet:jobexecutions/123456`
  gets the job execution with id `123456`. The response type is `JobExecution`.
  
* `jberet:jobexecutions/123456/stop`
  stops the job execution with id `123456`. No response is generated.
  
* `jberet:jobexecutions/123456/restart`
  restarts the job execution with id `123456`. any job parameters should 
  be passed as message body as `java.util.Properties`. The response type is
   `long` (job execution id).
  
* `jberet:jobexecutions/123456/abandon`
  abandons the job execution with id `123456`. No response is generated.
  
 
### Camel Batch Artifacts
 
 
#### `camelItemReader`
 
 Implementation of `javax.batch.api.chunk.ItemReader` that reads batch data 
 from Apache Camel endpoint. The source Camel endpoint is configured through
  batch property `endpoint` in job XML. For each read operation, this reader will wait
  up to the configured `timeout` milliseconds for data. Users may also configure
  `beanType` batch property to specify the expected Java type of the data.
  
 An example job.xml using camelItemReader:
 
```xml
 
  <job id="camelReaderTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0">
    <step id="camelReaderTest.step1">
      <chunk>
        <reader ref="camelItemReader">
          <properties>
            <property name="beanType" value="org.jberet.samples.wildfly.common.Movie"/>
            <property name="endpoint" value="#{jobParameters['endpoint']}"/>
            <property name="timeout" value="#{jobParameters['timeout']}"/>
          </properties>
        </reader>
```
 
#### `camelItemProcessor`

Implementation of `javax.batch.api.chunk.ItemProcessor` that processes 
batch data using Apache Camel component. The target Camel endpoint is configured
 through batch property `endpoint` in job XML. For example,
 
```xml

 <job id="camelReaderTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0">
   <step id="camelReaderTest.step1">
     <chunk>
       ... ...
       <processor ref="camelItemProcessor">
         <properties>
           <property name="endpoint" value="#{jobParameters['endpoint']}"/>
         </properties>
       </processor>
       ... ...
```

#### `camelItemWriter`

Implementation of `javax.batch.api.chunk.ItemWriter` that writes batch data
 to Apache Camel endpoint. The target Camel endpoint is configured through 
 batch property `endpoint` in job XML. For example,
 
```xml 

 <job id="camelWriterTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0">
   <step id="camelWriterTest.step1">
     <chunk>
       ... ...
       <writer ref="camelItemWriter">
         <properties>
           <property name="endpoint" value="#{jobParameters['endpoint']}"/>
         </properties>
       </writer>
     </chunk>
   </step>
 </job>
```
 
 
#### `camelJobListener`

An implementation of `javax.batch.api.listener.JobListener` that sends 
job execution events to a Camel endpoint. Two types of events are sent:

* beforeJob: sent before a job execution
* afterJob: sent after a job execution

The body of the message sent is the current `JobExecution`. Each message 
also contains a header to indicate the event type: its key is `eventType`,
 and value is either `beforeJob` or `afterJob`.
 
The target Camel endpoint is configured through batch property `endpoint` 
in job XML. For example,

```xml
 <job id="camelJobListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0">
    <listeners>
       <listener ref="camelJobListener">
           <properties>
               <property name="endpoint" value="#{jobParameters['endpoint']}"/>
           </properties>
       </listener>
    </listeners>
```

#### `camelStepListener`

An implementation of `javax.batch.api.listener.StepListener` that sends 
step execution events to a Camel endpoint. Two types of events are sent:

* beforeStep: sent before a step execution
* afterStep: sent after a step execution

The body of the message sent is the current `StepExecution`. Each message
 also contains a header to indicate the event type: its key is `eventType`,
  and value is either `beforeStep` or `afterStep`.
  
The target Camel endpoint is configured through batch property `endpoint`
 in job XML. For example,

```xml
 <job id="camelStepListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0">
   <step id="camelStepListenerTest.step1">
     <listeners>
       <listener ref="camelStepListener">
         <properties>
           <property name="endpoint" value="#{jobParameters['endpoint']}"/>
         </properties>
       </listener>
     </listeners>
     ... ...
   </step>
 </job>
```
 
#### `camelChunkListener`

An implementation of batch chunk listeners that sends chunk execution events
 to the configured Camel endpoint. The following are the chunk listener interfaces
  implemented by this class, and supported types of chunk execution events:
  
* `javax.batch.api.chunk.listener.ChunkListener`
    * `beforeChunk`
    * `onChunkError`
    * `afterChunk`
* `javax.batch.api.chunk.listener.ItemProcessListener`
    * `bef oreProcess`
    * `afterProcess`
    * `onProcessError`
* `javax.batch.api.chunk.listener.ItemReadListener`
    * `beforeRead`
    * `afterRead`
    * `onReadError`
* `javax.batch.api.chunk.listener.ItemWriteListener`
    * `beforeWrite`
    * `afterWrite`
    * `onWriteError`
* `javax.batch.api.chunk.listener.RetryProcessListener`
    * `onRetryProcessException`
* `javax.batch.api.chunk.listener.RetryReadListener`
    * `onRetryReadException`
* `javax.batch.api.chunk.listener.RetryWriteListener`
    * `onRetryWriteException`
* `javax.batch.api.chunk.listener.SkipProcessListener`
    * `onSkipProcessItem`
* `javax.batch.api.chunk.listener.SkipReadListener`
    * `onSkipReadItem`
* `javax.batch.api.chunk.listener.SkipWriteListener`
    * `onSkipWriteItem`
    
The body of the message sent is the current `ChunkExecutionInfo`. 
Each message also contains a header to indicate the event type: its key
 is `eventType`, and value is one from the above list.
 
The target Camel endpoint is configured through batch property `endpoint`
 in job XML. For example,

```xml
 <job id="camelChunkListenerTest" xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="1.0">
   <step id="camelChunkListenerTest.step1">
     <listeners>
       <listener ref="camelChunkListener">
         <properties>
           <property name="endpoint" value="#{jobParameters['endpoint']}"/>
         </properties>
       </listener>
     </listeners>
     ... ...
```

### Build 

```
mvn clean install
```

### Test App

[This test webapp](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/camelReaderWriter) uses all the features in jberet-camel.