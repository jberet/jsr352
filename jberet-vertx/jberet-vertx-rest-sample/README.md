curl http://localhost:8080/

curl http://localhost:8080/jobs/

curl -X POST http://localhost:8080/jobs/simple/start
curl -X POST 'http://localhost:8080/jobs/simple/start?sleepSeconds=4'
curl -X POST 'http://localhost:8080/jobs/simple/start?sleepSeconds=5'

curl 'http://localhost:8080/jobexecutions?jobExecutionId1=1&count=10'
curl 'http://localhost:8080/jobexecutions?jobExecutionId1=1'
curl http://localhost:8080/jobexecutions
curl 'http://localhost:8080/jobexecutions/running?jobName=simple'

curl http://localhost:8080/jobexecutions/1
curl http://localhost:8080/jobexecutions/2

curl -X POST http://localhost:8080/jobexecutions/2/stop

curl -X POST http://localhost:8080/jobexecutions/1/abandon

curl http://localhost:8080/jobexecutions/1/stepexecutions

curl http://localhost:8080/jobexecutions/1/stepexecutions/1

curl -X POST http://localhost:8080/jobexecutions/1/restart
curl -X POST http://localhost:8080/jobs/simple/restart

curl 'http://localhost:8080/jobinstances?jobName=simple&count=2'
curl 'http://localhost:8080/jobinstances?jobExecutionId=1'
curl 'http://localhost:8080/jobinstances?jobName=simple&count=2&start=1'
