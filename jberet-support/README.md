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
    * RestItemReader
    * RestItemWriter
    
* Fixed-width files and other formats supported by BeanIO:
    * BeanIOItemReader
    * BeanIOItemWriter
    
* CSV
    * CsvItemReader
    * CsvItemWriter
    * JacksonCsvItemReader
    * JacksonCsvItemWriter
    
* Excel
    * ExcelEventItemReader
    * ExcelStreamingItemReader
    * ExcelStreamingItemWriter
    * ExcelUserModelItemReader
    * ExcelUserModelItemWriter
    
* JDBC
    * JdbcItemReader
    * JdbcItemWriter
    
* MongoDB NoSQL
    * MongoItemReader
    * MongoItemWriter
    
* Messaging
    * JmsItemReader
    * JmsItemWriter
    * KafkaItemReader
    * KafkaItemWriter
    * HornetQItemReader
    * HornetQItemWriter
    
* JSON
    * JsonItemReader
    * JsonItemWriter

* XML
    * XmlItemReader
    * XmlItemWriter
    
* Jasper Reports
    * JasperReportsBatchlet
    
* JNDI ObjectFactory classes for custom JNDI resources in Java EE
    * MappingJsonFactoryObjectFactory
    * MongoClientObjectFactory
    * NoMappingJsonFactoryObjectFactory
    * XmlFactoryObjectFactory

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

