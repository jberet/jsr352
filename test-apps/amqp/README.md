# Overview

This test application demonstrates sending and receiving batch data as
AMQP messages, by using `org.jberet.support.io.JmsItemReader` and 
`org.jberet.support.io.JmsItemWriter`. This app requires a standalone
ActiveMQ Artemis broker to be running at `localhost:5672`. On the
messaging client side, Apache `qpid-jms-client` is used.
 
## Batch Job Definition 

 The batch job used in this application is defined in the following 2
 job xml files:
 
 * `META-INF/batch-jobs/amqpWriterTest.xml`
    * reads stock trade data from online resource via `csvItemReader` 
    (implemented in `jberet-support` module);
    * after 10 rows of (default `item-count`) stock trade data have been
      read, `jmsItemWriter` writes them as AMQP messages to ActiveMQ
      Artemis broker.
 
 * `META-INF/batch-jobs/amqpReaderTest.xml`
    * reads stock trade data as AMQP messages from ActiveMQ Artemis
      broker. These data have been sent to the broker as part of the test
      job `amqpWriterTest.xml`.
    * writes stock trade data as CSV output to `${jberet.tmp.dir}`, which
      by default is configured to `target/` directory.  
 
## Build and Run
 
To configure and start ActiveMQ Artemis standalone broker, follow
[ActiveMQ Artemis docs](https://activemq.apache.org/artemis/docs.html).
 Typically, it involves the following steps in a new installation:
 
 ``` 
 /Users/cfang/tools/artemis/bin > ./artemis create broker1
 cd ../broker1/bin/
 /Users/cfang/tools/artemis/broker1/bin > ./artemis run
 ```
 
After the broker has started successfully, take note of various supported
 protocols and their port bindings:
 
 ```
 23:17:23,361 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started Acceptor at 0.0.0.0:61616 for protocols [CORE,MQTT,AMQP,HORNETQ,STOMP,OPENWIRE]
 23:17:23,365 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started Acceptor at 0.0.0.0:5445 for protocols [HORNETQ,STOMP]
 23:17:23,369 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started Acceptor at 0.0.0.0:5672 for protocols [AMQP]
 23:17:23,375 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started Acceptor at 0.0.0.0:1883 for protocols [MQTT]
 23:17:23,377 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started Acceptor at 0.0.0.0:61613 for protocols [STOMP]
 23:17:23,377 INFO  [org.apache.activemq.artemis.core.server] AMQ221007: Server is now live
 23:17:23,378 INFO  [org.apache.activemq.artemis.core.server] AMQ221001: Apache ActiveMQ Artemis Message Broker version 1.3.0 [0.0.0.0, nodeID=c73e9aac-743b-11e6-a913-0b0b0b0b0b0b]
 23:17:23,984 INFO  [org.apache.activemq.artemis] AMQ241001: HTTP Server started at http://localhost:8161
 23:17:23,985 INFO  [org.apache.activemq.artemis] AMQ241002: Artemis Jolokia REST API available at http://localhost:8161/jolokia
 ```
 
To clean, build and run the test app:
 
 ``` 
 mvn clean install
 ```
 