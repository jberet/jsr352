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
