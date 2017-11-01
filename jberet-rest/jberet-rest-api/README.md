## Overview

This module defines REST API for interacting with batch job data, such as
job instances, job executions, step executions and metrics. 

### Documentation

Detailed REST API docs are available at 
[JBeret Docs Site](http://docs.jboss.org/jberet/latest/javadoc/jberet-rest-api/)

### Build and Tests

#### To build this module:

```
mvn clean install
```

#### To test REST API defined in this module in WildFly or JBoss EAP, 
go to [test-apps/restAPI](https://github.com/jberet/jsr352/tree/master/test-apps/restAPI),
and run:

```
mvn clean install -Pwildfly
```

In addition, all sample applications in 
[wildfly-jberet-samples](https://github.com/jberet/jsr352/tree/master/wildfly-jberet-samples)
module use these REST API to manage batch job executions.