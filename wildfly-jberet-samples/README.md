# Overview

 This module contains sample web applications to demonstrate the use of JBeret
 and JSR 352 (Batch for Java Application) in WildFly and JBoss EAP. These sample
 apps use batch components in `jberet-support` to simplify accessing common data
 sources and formats.
 
 In all sample applications, the test client performs batch job tasks via REST API
 defined and implemented in `jberet-rest-api` module. 
 
 ```
 Test --> JAX-RS Client API = = = > WildFly/EAP --> jberet-rest-api --> Batch API & JBeret
 ```
 
## Sample Applications

* [restWriter](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/restWriter)
    * uses `restItemWriter` to write batch data to destination resource via REST API.
    * uses `csvItemReader` to read data from online resource (movies-2012).
    * `restWriter` job inherits from reusable segments in parent JSL.
    * optionally include [jberet-ui](https://github.com/jberet/jsr352/tree/master/jberet-ui) in
    application package to provide a web front end for accessing batch job data.
* [restReader](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/restReader)
    * uses `restItemReader` to read data from online resource (movies-2012).
    * uses `csvItemWriter` to write data to CSV format.
* [batchProperty](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/batchProperty)
    * demonstrates the injection of batch properties of common java types, such as `int`, `long`, `List`,
    `Date`, `BigInteger`, `Map`, `Set`, arrays, etc.
* [csv2json](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/csv2json)
    * `csvItemReader` for reading data.
    * `jsonItemWriter` for writing data.
    * job inherits from parent JSL.
* [xml2json](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/xml2json)
    * `xmlItemReader` for reading data.
    * `jsonItemWriter` for writing data.
    * job inherits from parent JSL.
* [xml2jsonLookup](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/xml2jsonLookup)
    * same as `xml2json`, except that this sample looks up Jackson Json factory and XML factory in JNDI to
    improve resource efficiency.
* [csv2mongoLookup](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/csv2mongoLookup)
    * `mongoItemWriter` for writing data.
    * `csvItemReader` for reading data.
    * looks up MongoDB client in JNDI to improve resource efficiency.
* [excelstream2csv](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/excelstream2csv)
    * `excelEventItemReader` for reading data from Excel sheet.
    * `csvItemWriter` for writing data.
* [deserialization](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples/deserialization)
    * uses one custom class for checkpoint info, and another custom class for step persistent data, 
    to verify that they can be properly serialized and deserialized between job restart.
    * verifies the restartable job attribute.
    * verifies that a job whose job id differs from job xml file name can be started and restarted.
 
## Build, Deploy and Run
 
 A maven profile `wildfly` is defined to manage all WildFly-related tasks.
 In general, to clean and build the application, deploy it to WildFly or JBoss EAP, and 
 run all tests:
 
 ``` 
 mvn clean install -Pwildfly
 ```
 
To undeploy the application from WildFly or JBoss EAP:
 
 ``` 
 mvn clean -Pwildfly
 ```
 
 or
 
 ```
 mvn wildfly:undeploy
 ```
 