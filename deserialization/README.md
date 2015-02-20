This sample app consists of a chunk-type step with a reader and writer that reads and writes a series of numbers.
This job also uses one custom class for checkpoint info, and another custom class for step persistent data, to
verify that they can be properly serialized and deserialized between job restart.

The test class performs the following:

* starts a new job that will fail when the number in the reader reaches ${fail.on};
* in a separate test method, restart the same job that should continue from where it left in the previous run.

Various info is printed on the client side, and in server log to verify the progress of the job.

To more thoroughly verify serialization and deserialization behavior, configure WildFly to use jdbc job repository,
instead of the default in-memory job repository, through WildFly CLI.  The standalone.xml (or other server configuration
file) should look like:

    <subsystem xmlns="urn:jboss:domain:batch:1.0">
        <job-repository>
            <jdbc jndi-name="java:/jdbc/orderDB"/>
        </job-repository>
        <thread-pool>
            <max-threads count="50"/>
            <keepalive-time time="100" unit="milliseconds"/>
        </thread-pool>
    </subsystem>


The following is the detailed steps for running such test:

* start database server
* start WildFly
* mvn package
* deploy to WildFly
* run org.jberet.testapps.deserialization.DeserializationIT.startJob test, and the job execution will fail
* restart WildFly
* run org.jberet.testapps.deserialization.DeserializationIT.restartJob test, and the job execution will complete
