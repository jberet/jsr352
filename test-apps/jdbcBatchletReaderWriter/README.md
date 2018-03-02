# Overview

This test application demonstrates database-related batch processing, 
by using `org.jberet.support.io.JdbcBatchlet`, 
`org.jberet.support.io.JdbcItemReader` and 
`org.jberet.support.io.JdbcItemWriter`. This app requires the database
server running in a separate process before running any tests.

This test app is not a complete, working app, since some configurations
are not defined, such as database connection url, user, password,
database table definition, the select query for `jdbcItemReader`,
and the insert sql for `jdbcItemWriter`. Therefore, this test app
should be used as a generic template for testing database-related
batch operations. 
 
## Batch Job Definition 

 The batch job used in this application is defined in the following
 job xml files:
 
 * `META-INF/batch-jobs/jdbcBatchletReaderWriter.xml`
    * step1: run `jdbcBatchlet` to create the table, then proceed to step2,
             whether it succeeds or fails (if the table already exists)  
    * step2: `arrayItemReader` reads a series of numbers (dummy reader);
              `conversionItemProcessor` change the data to a map of data;
              `jdbcItemWriter` inserts the map data into db
    * step3: `jdbcItemReader` reads data from db;
             `mockItemWriter` displays data to the console