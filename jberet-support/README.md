## Overview

This module provides reusable batch components for common batch tasks. 
To use a `jberet-support` batch component, you will typically need to do
the following:

* include dependencies needed by the component;
* reference the component in job XML in either of the following ways:
    - by its CDI bean name, typically the short class name de-capitalized;
    - by the name declared for the component in `batch.xml` in your application;
    - by the fully-qualified class name of the component.
* configure any batch properties in job XML for the component.

The following is a list of reusable components in `jberet-support`:

* REST resources:
    * [RestItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/RestItemReader.java)
    * [RestItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/RestItemWriter.java)
    
* Fixed-width files and other formats supported by BeanIO:
    * [BeanIOItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/BeanIOItemReader.java)
    * [BeanIOItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/BeanIOItemWriter.java)
    
* CSV with supercsv or Jackson CSV
    * [CsvItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/CsvItemReader.java)
    * [CsvItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/CsvItemWriter.java)
    * [JacksonCsvItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JacksonCsvItemReader.java)
    * [JacksonCsvItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JacksonCsvItemWriter.java)
    
* Excel
    * [ExcelEventItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ExcelEventItemReader.java)
    * [ExcelStreamingItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ExcelStreamingItemReader.java)
    * [ExcelStreamingItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ExcelStreamingItemWriter.java)
    * [ExcelUserModelItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ExcelUserModelItemReader.java)
    * [ExcelUserModelItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ExcelUserModelItemWriter.java)
    
* JDBC
    * [JdbcItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JdbcItemReader.java)
    * [JdbcItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JdbcItemWriter.java)
    
* MongoDB NoSQL
    * [MongoItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/MongoItemReader.java)
    * [MongoItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/MongoItemWriter.java)
    
* Messaging
    * [JmsItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JmsItemReader.java)
    * [JmsItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JmsItemWriter.java)
    * [KafkaItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/KafkaItemReader.java)
    * [KafkaItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/KafkaItemWriter.java)
    * [ArtemisItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ArtemisItemReader.java)
    * [ArtemisItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/ArtemisItemWriter.java)
    
* JSON
    * [JsonItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JsonItemReader.java)
    * [JsonItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JsonItemWriter.java)

* XML
    * [XmlItemReader](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/XmlItemReader.java)
    * [XmlItemWriter](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/XmlItemWriter.java)
    
* Jasper Reports
    * [JasperReportsBatchlet](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/JasperReportsBatchlet.java)
    
* JNDI ObjectFactory classes for custom JNDI resources in Java EE
    * [MappingJsonFactoryObjectFactory](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/MappingJsonFactoryObjectFactory.java)
    * [MongoClientObjectFactory](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/MongoClientObjectFactory.java)
    * [NoMappingJsonFactoryObjectFactory](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/NoMappingJsonFactoryObjectFactory.java)
    * [XmlFactoryObjectFactory](https://github.com/jberet/jsr352/blob/master/jberet-support/src/main/java/org/jberet/support/io/XmlFactoryObjectFactory.java)

### Documentation

[JBeret Docs](http://docs.jboss.org/jberet/) contains JBeret User Guide and
 `jberet-support` Java Docs, which describes how to use and configure 
 `jberet-support` batch components.

### Build and Test

All tests in this module run batch jobs in Java SE environment, and therefore
no application server is needed. There are 2 maven profiles for different testing setup:

* `allTests`: include tests that need `MongoDB` server running, and so users will need to
start MongoDB server before running these tests;
* `default`: exclude all tests that need additional server running.

To clean, build, and run the default maven profile:

    mvn clean install

To clean, build, and run `allTests` maven profile:

    [in a separate terminal]
    $MONGO_HOME/bin/mongod
    
    mvn clean install -PallTests

### Other Examples

* [wildfly-jberet-samples module](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples)
contains sample web applications that use `jberet-support` components in Java EE, WildFly and JBoss EAP
environment.

* [tests directory](https://github.com/jberet/jsr352/tree/master/jberet-support/src/test)
contains tests for every `jberet-support` component running in Java SE environment.

* Search in [JBeret JIRA Issue Track](https://issues.jboss.org/issues/?jql=project%20%3D%20JBERET)

