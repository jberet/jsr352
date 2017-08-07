
bin/domain.sh --host-config=host-master.xml -Djboss.domain.base.dir=domain1

bin/domain.sh --host-config=host-slave.xml -Djboss.domain.base.dir=host1 -Djboss.domain.master.address=127.0.0.1 -Djboss.management.native.port=9991

./add-user.sh 
guest
WildFly11.0

[standalone@localhost:9990 /] 
jms-queue add --queue-address=partitionQueue --entries=jms/partitionQueue,java:jboss/exported/jms/partitionQueue

[standalone@localhost:9990 /] 
jms-topic add --topic-address=stopRequestTopic --entries=jms/stopRequestTopic,java:jboss/exported/jms/jms/stopRequestTopic

<jms-queue name="partitionQueue" entries="jms/partitionQueue java:jboss/exported/jms/partitionQueue"/>
<jms-topic name="stopRequestTopic" entries="jms/stopRequestTopic java:jboss/exported/jms/jms/stopRequestTopic"/>
                
                
https://issues.jboss.org/browse/JBEAP-773
<module-option name="unauthenticatedIdentity" value="guest"/>

<subsystem xmlns="urn:jboss:domain:messaging-activemq:1.1">
            <server name="default">
                <security enabled="false"/>
                <cluster password="${jboss.messaging.cluster.password:CHANGE ME!!}"/>