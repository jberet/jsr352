<p align="center">
   <img src="./images/jberet_logo_600px.png" alt="JBeret logo" title="JBeret" height="150" width="600"/>
</p>


JBeret is an implementation of [Jakarta Batch](https://jakarta.ee/specifications/batch/). It is also included in [WildFly, the new and improved JBoss Application Server](https://wildfly.org/) to provide portable batch processing support in Jakarta EE environment. 

#### Build JBeret
To build and run default set of tests:
    
    mvn install

Some tests require additional steps and thus are not included in the default run. For instance, MongoDB-related tests
need to start MongoDB first. To build and run all tests:
    
    # start MongoDB database
    mongod
    
    # build JBeret, activate allTests maven profile to run all tests
    mvn install -DallTests
    
Some tests involves very large data set (e.g., over 1 million rows of CSV or Excel data), and may
cause memory errors in some machines:

    OutOfMemoryError: unable to create new native thread

Increase `ulimit` to avoid such errors. For example,

    ulimit -u 2048add

#### JBeret Modules:
* [jberet-core](https://github.com/jberet/jsr352/tree/master/jberet-core): core batch runtime engine
* [jberet-se](https://github.com/jberet/jsr352/tree/master/jberet-se): impl classes specific to Java SE runtime environment
* [jberet-support](https://github.com/jberet/jberet-support): a collection of reusable batch readers and writers (e.g., CSV, fixed length, Excel, Json, XML, Mongo, JDBC, JMS, HornetQ, PDF, etc) for batch applications, and JNDI support
* [jberet-rest-api](https://github.com/jberet/jberet-rest): REST API for batch job management
* [jberet-ui](https://github.com/jberet/jberet-ui): front-end UI web app for batch job management
* [test-apps](https://github.com/jberet/jsr352/tree/master/test-apps): test applications
* [tck-porting-impl](https://github.com/jberet/jberet-tck-porting): support running [Jakarta Batch TCK](https://jakarta.ee/specifications/batch/) with JBeret in Java SE
* [wildfly-jberet-samples](https://github.com/jberet/jberet-wildfly-samples): Sample batch processing apps that can be deployed to WildFly or JBoss EAP 7
* [quarkus-jberet](https://github.com/quarkiverse/quarkus-jberet): The Quarkus JBeret Extension adds support for Jakarta Batch applications

#### 3rd-party & Community Extensions:
* [JBoss Tools for Batch (Eclipse extensions, Wizards, Visual editing, etc)](https://tools.jboss.org/features/batch.html)
* [jberet-eap6](https://github.com/fcorneli/jberet-eap6)
* [jberetweb, job repository viewer](https://github.com/lbtc-xxx/jberetweb)

#### Project Resources:
* [JBeret Issues & Bugs](https://issues.jboss.org/browse/JBERET-55?jql=project%20%3D%20JBERET), [Issues Awaiting Volunteers](https://issues.jboss.org/browse/JBERET-143?jql=project%20%3D%20JBERET%20AND%20fixVersion%20%3D%20%22Awaiting%20Volunteers%22)
* [JBeret Documentation](https://docs.jboss.org/jberet/)
* [JBoss EAP Quickstarts for Batch Processing](https://github.com/jboss-developer/jboss-eap-quickstarts)
* [JBeret-dev Mailing List](https://lists.jboss.org/mailman/listinfo/jberet-dev)
* [WildFly Forum](https://groups.google.com/g/wildfly)
* [JBeret Forum](https://developer.jboss.org/en/jberet/)
* [JBoss Batch API project](https://github.com/jboss/jboss-batch-api_spec)
* [Jakarta Batch Expert Group Discussion](https://github.com/eclipse-ee4j/batch-api/issues)
* Download JBeret jars and distro zip from [JBoss.org nexus](https://repository.jboss.org/nexus/index.html#nexus-search;quick~jberet)

#### Batch sample & test applications:
  - <https://github.com/chengfang/wildfly-samples/tree/master/jberet>, web apps that demonstrate the following:
    + JsonItemReader, JsonItemWriter
    + CsvItemReader, CsvItemWriter
    + XmlItemReader, XmlItemWriter
    + MongoItemReader, MongoItemWriter
    + JNDI lookup of Jackson JsonFactory, MappingJsonFactory & XmlFactory in WildFly for batch reader and writer
    + JNDI lookup of MongoClient in WildFly
    + job xml files showing the use of various reader/writer configuration properties
    + jberet-support module can be installed in WildFly and referenced by multiple apps via either MANIFEST.MF or jboss-deployment-structure.xml
  - <https://github.com/jberet/jsr352/tree/master/test-apps>
    + test apps running in Java SE environment to verify core batch requirements
    + test apps running in Java SE environment to verify additional JBeret features (inheritance, scripting support, infinispan job repository, etc)

#### org.jberet artifacts may be retrieved from Maven Central or JBoss Public Repository
    <repositories>
        <repository>
            <id>jboss-public-repository-group</id>
            <name>JBoss Public Repository Group</name>
            <url>https://repository.jboss.org/nexus/content/groups/public/</url>
        </repository>
    </repositories>
    ...
    <dependencies>
            <dependency>
                <groupId>jakarta.batch</groupId>
                <artifactId>jakarta.batch-api</artifactId>
                <version>${version.jakarta.batch.batch-api}</version>
                <scope>provided</scope>
            </dependency>

        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-core</artifactId>
            <version>1.0.2.Final</version> <!-- replace it with the desired version -->
        </dependency>
        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-support</artifactId>
            <version>1.0.2.Final</version> <!-- replace it with the desired version -->
        </dependency>

#### Batch application dependencies
##### Minimal application dependencies:
        <dependency>
                <groupId>jakarta.batch</groupId>
                <artifactId>jakarta.batch-api</artifactId>
            </dependency>
        <dependency>
            <groupId>javax.inject</groupId>
            <artifactId>javax.inject</artifactId>
        </dependency>
        <dependency>
            <groupId>javax.enterprise</groupId>
            <artifactId>cdi-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jboss.spec.javax.transaction</groupId>
            <artifactId>jboss-transaction-api_1.2_spec</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jboss.marshalling</groupId>
            <artifactId>jboss-marshalling</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jboss.logging</groupId>
            <artifactId>jboss-logging</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jboss.weld</groupId>
            <artifactId>weld-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.wildfly.security</groupId>
            <artifactId>wildfly-security-manager</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </dependency>
        
A note on webapp or Jakarta EE application packaging: Jakarta EE API jars
are already available in the appserver, and should not be included in WAR, JAR, or EAR files. Their maven dependency
scope should be set to `provided`. In addition, if the application is deployed to JBoss EAP or WildFly, almost all of
the above dependencies are already available as JBoss modules, and should not be duplicated in application package.
        
##### The following is also required for Java SE batch applications (h2 can be omitted when using in-memory batch job repository):
        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-se</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jboss.weld.se</groupId>
            <artifactId>weld-se</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        
##### Optional application dependencies depending on application usage:
        <!-- any JDBC driver jars, e.g., h2, when using jdbc batch job repository -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>

         <!-- infinispan and jgroups jars, when infinispan job repository is used.
              Additional infinispan cachestore jars (e.g., infinispan-cachestore-jdbc, infinispan-cachestore-mongodb,
              infinispan-cachestore-leveldb, infinispan-cachestore-rest, infinispan-cachestore-cassandra, etc) may be
              needed if such a cachestore is used. -->
         <dependency>
             <groupId>org.infinispan</groupId>
             <artifactId>infinispan-core</artifactId>
         </dependency>
         <dependency>
             <groupId>org.infinispan</groupId>
             <artifactId>infinispan-commons</artifactId>
         </dependency>
         <dependency>
             <groupId>org.jgroups</groupId>
             <artifactId>jgroups</artifactId>
         </dependency>

         <!-- MongoDB jars, when MongoDB job repository is used -->
         <dependency>
             <groupId>org.mongodb</groupId>
             <artifactId>mongo-java-driver</artifactId>
             <version>${version.org.mongodb.mongo-java-driver}</version>
             <scope>provided</scope>
         </dependency>

        <!-- For Weld 2.2.2.Final or later, Jandex is required for annotation processing -->
        <dependency>
            <groupId>org.jboss</groupId>
            <artifactId>jandex</artifactId>
        </dependency>
        
        <!-- replace Java built-in StAX provider with aalto-xml or woodstox
             (woodstox dependencies not shown here)
        -->
        <dependency>
            <groupId>com.fasterxml</groupId>
            <artifactId>aalto-xml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.codehaus.woodstox</groupId>
            <artifactId>stax2-api</artifactId>
        </dependency>
        
        <!-- jberet-support includes common reusable batch ItemReader & ItemWriter classes for
             various data types such as CSV, XML, JSON, Fixed length, Excel, MongoDB, JDBC, JMS, HornetQ, etc.
             The application should further provide appropriate transitive dependencies from 
             jberet-support, depending on its usage.
        -->
        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-support</artifactId>
        </dependency>
