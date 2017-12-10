This module contains classes to support Vert.x-based REST API for JBeret.

## Usage Examples

To use Vert.x-based REST API in your JBeret batch application, simply configure the Vert.x router by calling
`org.jberet.vertx.rest.JBeretRouterConfig.config` method:

```java
Router router = Router.router(vertx);
JBeretRouterConfig.config(router);
vertx.createHttpServer().requestHandler(router::accept).listen(8080);

```

## URI to REST API Mapping

The following REST API routes are currently supported:

* Default mapping
    * HTTP Method: GET
    * URI Pattern: /
    * Query Params: None
    * Return: default return value
    * Examples:
        * http://localhost:8080/

* Get jobs
    * HTTP Method: GET
    * URI Pattern: /jobs
    * Query Params: None
    * Return: JSON array of jobs
    * Examples:
        * http://localhost:8080/jobs

* Start a job execution with a job name
    * HTTP Method: POST
    * URI Pattern: /jobs/:jobXmlName/start
    * Query Params: 0 or more job parameters
    * Return: job execution as JSON
    * Examples:
        * http://localhost:8080/jobs/simple/start
        * http://localhost:8080/jobs/simple/start?sleepSeconds=4&foo=bar

* Schedule a job execution with a job name
    * HTTP Method: POST
    * URI Pattern: /jobs/:jobXmlName/schedule
    * Query Params:
        * delay: required, int number indicating the number of minutes to delay before starting job execution
        * periodic: flag to control whether the schedule is recurring or not, optional and defaults to false
    * Return: job schedule as JSON
    * Examples:
        * http://localhost:8080/jobs/simple/schedule?delay=1
        * http://localhost:8080/jobs/simple/schedule?delay=1&periodic=true

* Restart the most recently failed or stopped job execution belonging to the job name
    * HTTP Method: POST
    * URI Pattern: /jobs/:jobXmlName/restart
    * Query Params: 0 or more job parameters to override the corresponding original job parameters
    * Return: job execution as JSON
    * Examples:
        * http://localhost:8080/jobs/simple/restart
        * http://localhost:8080/jobs/simple/restart?sleepSeconds=5&foo=bar
        
* Get job instances
    * HTTP Method: GET
    * URI Pattern: /jobinstances
    * Query Params:
        * jobName: the job name used to get the associated job instances, required unless jobExecutionId is present
        * start: the offset position in the list of all eligible job instances to include, optional and defaults to 0
        * count: the number of job instances to return, optional and defaults to all job instances
        * jobExecutionId: the job execution id used to get the associated job instance. This param should not be used along with jobName, start, or count.
    * Return: JSON array of job instances
    * Examples:
        * http://localhost:8080/jobinstances?jobName=simple&count=2
        * http://localhost:8080/jobinstances?jobExecutionId=1
        * http://localhost:8080/jobinstances?jobName=simple&count=2&start=1
        * http://localhost:8080/jobinstances/count?jobName=simple

* Count the number of job instances
    * HTTP Method: GET
    * URI Pattern: /jobinstances/count
    * Query Params:
        * jobName: the job name used to get the associated job instances, required.
        * Return: number of job instances
    * Examples:
        * http://localhost:8080/jobinstances/count?jobName=simple
        
* Get job executions
    * HTTP Method: GET
    * URI Pattern: /jobexecutions
    * Query Params:
        * count: the number of job executions to return, optional and defaults to all matching job executions
        * jobExecutionId1: the job execution id whose sibling job executions will be returned, optional and defaults to unspecified
    * Return: JSON array of job executions
    * Examples:
        * http://localhost:8080/jobexecutions
        * http://localhost:8080/jobexecutions?count=5
        * http://localhost:8080/jobexecutions?jobExecutionId1=2
        * http://localhost:8080/jobexecutions?jobExecutionId1=1&count=10
        
* Get running job executions associated with a job name
    * HTTP Method: GET
    * URI Pattern: /jobexecutions/running
    * Query Params:
        * jobName: the job name used to get the associated job instances, required.
    * Return: JSON array of job executions
    * Examples:
        * http://localhost:8080/jobexecutions/running?jobName=simple'
        
* Get job execution by id
    * HTTP Method: GET
    * URI Pattern: /jobexecutions/:jobExecutionId
    * Query Params: None
    * Return: job execution as JSON
    * Examples:
        * http://localhost:8080/jobexecutions/1
        * http://localhost:8080/jobexecutions/2
        
* Get step executions belonging to a particular job execution
    * HTTP Method: GET
    * URI Pattern: /jobexecutions/:jobExecutionId/stepexecutions
    * Query Params: None
    * Return: JSON array of step executions
    * Examples:
        * http://localhost:8080/jobexecutions/1/stepexecutions
        * http://localhost:8080/jobexecutions/15/stepexecutions
        
* Get the step execution by job execution id and step execution id
    * HTTP Method: GET
    * URI Pattern: /jobexecutions/:jobExecutionId/stepexecutions/:stepExecutionId
    * Query Params: None
    * Return: step execution as JSON
    * Examples:
        * http://localhost:8080/jobexecutions/1/stepexecutions/1

* Abandon a job execution
    * HTTP Method: POST
    * URI Pattern: /jobexecutions/:jobExecutionId/abandon
    * Query Params: None
    * Return: void
    * Examples:
        * http://localhost:8080/jobexecutions/1/abandon
        
* Stop a job execution
    * HTTP Method: POST
    * URI Pattern: /jobexecutions/:jobExecutionId/stop
    * Query Params: None
    * Return: void
    * Examples:
        * http://localhost:8080/jobexecutions/2/stop
        
* Restart a failed or stopped job execution
    * HTTP Method: POST
    * URI Pattern: /jobexecutions/:jobExecutionId/restart
    * Query Params: 0 or more job parameters to override the corresponding original job parameters
    * Return: job execution as JSON
    * Examples:
        * http://localhost:8080/jobexecutions/1/restart
        * http://localhost:8080/jobexecutions/1/restart?sleepSeconds=3&foo=buzz

* Get all job schedules
    * HTTP Method: GET
    * URI Pattern: /schedules
    * Query Params: None
    * Return: JSON array of job schedules
    * Examples:
        * http://localhost:8080/schedules
        
* Get job schedule by id
    * HTTP Method: GET
    * URI Pattern: /schedules/:scheduleId
    * Query Params: None
    * Return: job schedule as JSON
    * Examples:
        * http://localhost:8080/schedules/2
        
* Get all timezone values available to job schedules
    * HTTP Method: GET
    * URI Pattern: /schedules/timezones
    * Query Params: None
    * Return: JSON array of all timezone values
    * Examples:
        * http://localhost:8080/schedules/timezones

* Get supported extra features as a string array, currently return []
    * HTTP Method: GET
    * URI Pattern: /schedules/features
    * Query Params: None
    * Return: JSON array, currently return empty array
    * Examples:
        * http://localhost:8080/schedules/features
        
* Cancel a job schedule
    * HTTP Method: POST
    * URI Pattern: /schedules/:scheduleId/cancel
    * Query Params: None
    * Return: true if the job schedule is cancelled successfully; false otherwise
    * Examples:
        * http://localhost:8080/schedules/0/cancel
        
* Delete a job schedule
    * HTTP Method: DELETE
    * URI Pattern: /schedules/:scheduleId
    * Query Params: None
    * Return: void
    * Examples:
        * http://localhost:8080/schedules/0
        
## JBeret REST API Implementation Based on JAX-RS

Another JBeret module, jberet-rest-api, implements the same set of REST API based on JAX-RS.
See <a href="http://docs.jboss.org/jberet/latest/rest-doc/">jberet-rest-api docs</a> for details. 