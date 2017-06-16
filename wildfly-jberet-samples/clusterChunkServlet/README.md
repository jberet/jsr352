
./standalone.sh -c standalone-full-ha.xml -Djboss.messaging.cluster.password=hello

./add-user.sh 
guest
WildFly11.0

[standalone@localhost:9990 /] 
jms-queue add --queue-address=partitionQueue --entries=jms/partitionQueue,java:jboss/exported/jms/partitionQueue

[standalone@localhost:9990 /] 
jms-topic add --topic-address=stopRequestTopic --entries=jms/stopRequestTopic,java:jboss/exported/jms/jms/stopRequestTopic

https://issues.jboss.org/browse/JBEAP-773
<module-option name="unauthenticatedIdentity" value="guest"/>

<subsystem xmlns="urn:jboss:domain:messaging-activemq:1.1">
            <server name="default">
                <security enabled="false"/>
                <cluster password="${jboss.messaging.cluster.password:CHANGE ME!!}"/>