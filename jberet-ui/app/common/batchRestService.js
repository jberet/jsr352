'use strict';

var batchRestService = function($http) {
    /**
     * URL for JBeret REST API.
     * The URL string includes hostname, port number, application context path, and REST API path.
     * Users need to modify this property for their environment.
     */
    var restApiUrl = 'http://localhost:8080/restAPI/api';

    var jobsUrl = restApiUrl + '/jobs';
    var jobInstancesUrl = restApiUrl + '/jobinstances';
    var jobExecutionsUrl = restApiUrl + '/jobexecutions';

    this.getJobs = function () {
        return $http.get(jobsUrl);
    };

    this.startJob = function (jobXmlName, jobParameters) {
        return $http.post(jobsUrl + '/' + jobXmlName + '/start', jobParameters);
    };

    this.getJobInstances = function (jobName) {
        if (jobName) {
            return $http.get(jobInstancesUrl + '?jobName=' + jobName);
        }
        return $http.get(jobInstancesUrl);
    };

    this.getJobExecutions = function (running, jobName, jobInstanceId, jobExecutionId1) {
        if (running && jobName) {
            return $http.get(jobExecutionsUrl + '/running?jobName=' + jobName);
        } else if (jobExecutionId1) {
            return $http.get(jobExecutionsUrl + '?jobExecutionId1=' + jobExecutionId1 + '&jobInstanceId=' +
                (jobInstanceId ? jobInstanceId : 0));
        } else {
            return $http.get(jobExecutionsUrl);
        }
    };

    this.getJobExecution = function (jobExecutionId) {
        return $http.get(this.urlForJobExecution(jobExecutionId));
    };

    this.stopJobExecution = function (jobExecutionId) {
        return $http.post(this.urlForJobExecution(jobExecutionId) + '/stop', null);
    };

    this.restartJobExecution = function (jobExecutionId, jobParameters) {
        return $http.post(this.urlForJobExecution(jobExecutionId) + '/restart', jobParameters);
    };

    this.abandonJobExecution = function (jobExecutionId) {
        return $http.post(this.urlForJobExecution(jobExecutionId) + '/abandon', null);
    };

    this.getStepExecutions = function (jobExecutionUrl) {
        return $http.get(jobExecutionUrl + '/stepexecutions');
    };

    this.getStepExecution = function (jobExecutionId, stepExecutionId) {
        return $http.get(this.urlForJobExecution(jobExecutionId) + '/stepexecutions/' + stepExecutionId);
    };

    this.urlForJobExecution = function (jobExecutionId) {
        return jobExecutionsUrl + '/' + jobExecutionId;
    };
};

//batchRestService.$inject = ['$http'];

module.exports = batchRestService;