 This sample application demonstrates the use of `org.jberet.support.io.RestItemWriter`,
 which writes batch data via REST API to remote resources.
 
 
 The batch job used in this application is defined in `restWriter.xml`, which
 inherits from a super JSL file `csvItemReader.xml`. The batch job performs the
 following:
 
 * reads movies data from online resource via `csvItemReader` (implemented in
 `jberet-support` module;
 * movies data are converted into `org.jberet.samples.wildfly.common.Movie`
 objects and returned from `csvItemReader`;
 * after 10 (default item-count in `restWriter.xml`) movie objects have been
 read, `restItemWriter` writes them the remote destination via REST API
 `POST` or `PUT` opertions.
 
  
 `restItemWriter` can be configured with the following batch properties:
 
 * `restUrl`: The base URI for the REST call
 * `httpMethod`: HTTP method to use in the REST call to write data
 * `mediaType`: Media type to use in the REST call to write data
 
 
 
To clean and build the application, deploy it to WildFly or JBoss EAP, and 
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
 
 To incluee `jberet-ui` module into the WAR archive, run with `includeUI`
 maven profile. `jberet-ui` module adds a front-end UI for interacting with 
 batch job data.
 
 ```
 mvn clean install -PincludeUI
 ```
 
 To access `jberet-ui` web pages, go to http://localhost:8080/restWriter/#/jobs
 