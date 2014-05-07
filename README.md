JBeret is an implementation of [JSR 352 (Batch Applications for the Java Platform)](https://java.net/projects/jbatch). It is also included in [WildFly, the new and improved JBoss Application Server](http://wildfly.org/) to provide portable batch processing support in Java EE environment. 

####Sub-modules:
* [jberet-core](https://github.com/jberet/jsr352/tree/master/jberet-core): core batch runtime engine
* [jberet-se](https://github.com/jberet/jsr352/tree/master/jberet-se): impl classes specific to Java SE runtime environment
* [jberet-support](https://github.com/jberet/jsr352/tree/master/jberet-support): a collection of reusable batch readers and writers (e.g., CSV, fixed length, Json, XML, Mongo, etc) for batch applications, and JNDI support
* [jberet-distribution](https://github.com/jberet/jsr352/tree/master/jberet-distribution): produces a zip distribution for Java SE
* [jberet-jpa-repository](https://github.com/jberet/jsr352/tree/master/jberet-jpa-repository): batch job repository implemented with JPA (incomplete)
* [test-apps](https://github.com/jberet/jsr352/tree/master/test-apps): test applications
* [tck-porting-impl](https://github.com/jberet/jsr352/tree/master/test-apps): support running [JSR 352 TCK](https://java.net/projects/jbatch/downloads) with JBeret in Java SE

####Project Resources:
* [JBeret Issues & Bugs](https://issues.jboss.org/browse/JBERET-55?jql=project%20%3D%20JBERET)
* [WildFly Forum](https://community.jboss.org/en/wildfly?view=discussions)
* [JBeret Forum](https://community.jboss.org/en/jberet/)
* [JSR 352 Expert Group Discussion](https://java.net/projects/jbatch/lists/public/archive)
* [JBoss Batch API project](https://github.com/jboss/jboss-batch-api_spec)
* Download JBeret jars and distro zip from [JBoss.org nexus](https://repository.jboss.org/nexus/index.html#nexus-search;quick~jberet)
* Additional JBeret project info on [ohloh.net](https://www.ohloh.net/p/jberet)

####Batch sample & test applications:
  - <https://github.com/chengfang/wildfly-samples/tree/master/jberet>, web apps that demonstrate the following:
    + JsonItemReader, JsonItemWriter
    + CsvItemReader, CsvItemWriter
    + XmlItemReader, XmlItemWriter
    + MongoItemReader, MongoItemWriter
    + JNDI lookup of Jackson JsonFactory, MappingJsonFactory & XmlFactory in WildFly for batch reader and writer
    + JNDI lookup of MongoClient in WildFly
    + job xml files showing the use of various reader/writer configuration properties
    + jberet-support module can be installed in WildFly and referenced by multiple apps via either MANIFEST.MF or jboss-deployment-structure.xml
  - <https://github.com/jberet/jsr352/tree/master/jberet-support/src/test>
    + comprehensive tests for implemented batch readers and writers
  - <https://github.com/jberet/jsr352/tree/master/test-apps>
    + test apps running in Java SE environment to verify core batch requirements
    + test apps running in Java SE environment to verify additional JBeret features (inheritance, scripting support, etc)

####How to reference org.jberet artifacts in maven pom.xml
    <repositories>
        <repository>
            <id>jboss-public-repository-group</id>
            <name>JBoss Public Repository Group</name>
            <url>http://repository.jboss.org/nexus/content/groups/public/</url>
        </repository>
    </repositories>
    ...
    <dependencies>
        <dependency>
            <groupId>org.jboss.spec.javax.batch</groupId>
            <artifactId>jboss-batch-api_1.0_spec</artifactId>
            <version>1.0.0.Final</version>
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
