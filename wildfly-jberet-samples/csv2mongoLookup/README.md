This sample webapp demonstrates the following features in jberet and jberet-support
(https://issues.jboss.org/browse/JBERET-48?jql=project%20%3D%20JBERET):

* reads CSV data from online source, and binds record to POJO with `org.jberet.support.io.CsvItemReader`

* writes POJO beans to MongoDB with `org.jberet.support.io.MongoItemWriter`

* `MongoClient` resource is configured in WildFly server and obtained via JNDI

* jberet-support and its dependencies are installed in WildFly server, and referenced by webapp via `MANIFEST.MF`

While in `wildfly-jberet-samples` directory, to build:

    mvn clean install -pl csv2mongoLookup,jberet-samples-common,.

To start MongoDB instance and drop collection (movies.out) to avoid data conflict during insertion:

    cd $MONGO_HOME/bin
    ./mongod
    [open another termional to start mongo shell]
    ./mongo
    use testData
    show collections
    db.movies.out.drop()

To deploy to WildFly:

    $JBOSS_HOME/bin/jboss-cli.sh -c "deploy --force csv2mongoLookup/target/csv2mongoLookup.war"

To run the webapp and start the batch job:

    curl http://localhost:8080/csv2mongoLookup/api/jobs/csv2mongoLookup/start

(`csv2mongoLookup` after 8080/ is the context path, which is the war file name without `.war` extension.
`csv2mongoLookup` after `jobs` is the job name, which is the base name of `META-INF/batch-jobs/csv2mongoLookup.xml`)

See WildFly server log for more details.
