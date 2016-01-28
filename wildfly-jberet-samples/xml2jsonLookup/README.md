This sample webapp starts a batch job that reads XML data from online source, binds each data record to
POJO (Movie bean), and serializes them to JSON output file. The job is configured to look up Jackson
`JsonFactory` and `XmlFactory` from WildFly JNDI. jberet-support module and its dependencies are installed in
WildFly and referenced by this webapp via `WEB-INF/jboss-deployment-structure.xml`. 
See https://issues.jboss.org/browse/JBERET-47

While in `wildfly-jberet-samples` directory, to build:

    mvn clean install -pl xml2jsonLookup,jberet-samples-common,.

To deploy to WildFly:

    $JBOSS_HOME/bin/jboss-cli.sh -c "deploy --force xml2jsonLookup/target/xml2jsonLookup.war"

To run the webapp and start the batch job:

    curl http://localhost:8080/xml2jsonLookup/api/jobs/xml2jsonLookup/start

(`xml2jsonLookup` after 8080/ is the context path, which is the war file name without `.war` extension.
`xml2jsonLookup` after `jobs` is the job name, which is the base name of `META-INF/batch-jobs/xml2jsonLookup.xml`)

See WildFly server log for more details.
