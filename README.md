JBeret is an implementation of [JSR 352 (Batch Applications for the Java Platform)](https://java.net/projects/jbatch). It is also included in [WildFly, the new and improved JBoss Application Server](http://wildfly.org/) to provide portable batch processing support in Java EE environment. 

####It includes the following sub-modules:
* [jberet-core](https://github.com/jberet/jsr352/tree/master/jberet-core): core batch runtime engine
* [jberet-se](https://github.com/jberet/jsr352/tree/master/jberet-se): impl classes specific to Java SE runtime environment
* [jberet-support](https://github.com/jberet/jsr352/tree/master/jberet-support): a collection of reusable batch artifacts for batch applications, and JNDI support
* [jberet-distribution](https://github.com/jberet/jsr352/tree/master/jberet-distribution): produces a zip distribution for Java SE
* [jberet-jpa-repository](https://github.com/jberet/jsr352/tree/master/jberet-jpa-repository): batch job repository implemented with JPA
* [test-apps](https://github.com/jberet/jsr352/tree/master/test-apps): test applications
* [tck-porting-impl](https://github.com/jberet/jsr352/tree/master/test-apps): support running [JSR 352 TCK](https://java.net/projects/jbatch/downloads) with JBeret in Java SE

####Project Resources:
* [JBeret Issues](https://issues.jboss.org/browse/JBERET-55?jql=project%20%3D%20JBERET)
* [WildFly Forum](https://community.jboss.org/en/wildfly?view=discussions)
* [JBeret Forum](https://community.jboss.org/en/jberet/)
* [JSR 352 Expert Group Discussion](https://java.net/projects/jbatch/lists/public/archive)
* Batch sample applications:
  - <https://github.com/chengfang/wildfly-samples/tree/master/jberet>
  - <https://github.com/jberet/jsr352/tree/master/jberet-support/src/test>
  - <https://github.com/jberet/jsr352/tree/master/test-apps>
* Download from [JBoss.org nexus](https://repository.jboss.org/nexus/index.html#nexus-search;quick~jberet)
* More JBeret project info on [ohloh.net](https://www.ohloh.net/p/jberet)

####How to reference org.jberet artifacts in maven pom.xml
    <repositories>
        <repository>
            <id>jboss-public-repository-group</id>
            <name>JBoss Public Repository Group</name>
            <url>http://repository.jboss.org/nexus/content/groups/public/</url>
            <layout>default</layout>
            <releases>
                <enabled>true</enabled>
                <updatePolicy>never</updatePolicy>
            </releases>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>never</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
    ...
    <dependencies>
        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-core</artifactId>
            <version>1.0.1.CR1</version> <!-- replace it with the desired version -->
        </dependency>
        <dependency>
            <groupId>org.jberet</groupId>
            <artifactId>jberet-support</artifactId>
            <version>1.0.1.CR1</version> <!-- replace it with the desired version -->
        </dependency>
