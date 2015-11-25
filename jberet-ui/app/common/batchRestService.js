'use strict';

var batchRestService = function($http) {
    /**
     * Token for the URL for JBeret REST API. Its value is obtained from either gulp command line args,
     * application config.json file, environment variable, or default value.
     * See gulpfile.js for more details.
     * The URL string includes hostname, port number, application context path, and REST API path.
     * Users need to modify this property for their environment.
     */
    var restApiUrl = '/* @echo __REST_URL__ */';

    var jobsUrl = restApiUrl + '/jobs';
    var jobInstancesUrl = restApiUrl + '/jobinstances';
    var jobExecutionsUrl = restApiUrl + '/jobexecutions';

    this.getJobs = function () {
        return $http.get(jobsUrl);
    };

    this.startJob = function (jobXmlName, jobParameters) {
        return $http.post(jobsUrl + '/' + encodeURIComponent(jobXmlName) + '/start', jobParameters);
    };

    this.getJobInstances = function (jobName, start, count) {
        var url = jobInstancesUrl;
        var sep = '?';

        if (jobName) {
            url += sep + 'jobName=' + encodeURIComponent(jobName);
            sep = '&';
        }
        if(start) {
            url += sep + 'start=' + start;
            sep = '&';
        }
        if(count) {
            url += sep + 'count=' + count;
        }
        return $http.get(url);
    };

    this.getJobExecutions = function (count, running, jobName, jobInstanceId, jobExecutionId1) {
        var url = jobExecutionsUrl;
        if (running && jobName) {
            url += '/running?jobName=' + encodeURIComponent(jobName);
            return $http.get(url);
        }
        if (jobExecutionId1) {
            url += '?jobExecutionId1=' + jobExecutionId1 + '&jobInstanceId=' + (jobInstanceId ? jobInstanceId : 0);
            if(count && count > 0) {
                url += '&count=' + count;
            }
        } else if (count && count > 0) {
            url += '?count=' + count;
        }
        return $http.get(url);
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