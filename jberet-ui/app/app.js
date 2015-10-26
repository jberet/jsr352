'use strict';

// Declare app level module which depends on views, and components
angular.module('jberetUI',
    ['ui.router', 'jberetUI.jobs', 'jberetUI.jobinstances', 'jberetUI.jobexecutions', 'jberetUI.details',
        'jberetUI.stepexecution', 'jberetUI.version'])

    .config(['$urlRouterProvider', function ($urlRouterProvider) {
        $urlRouterProvider.otherwise('/jobs');
    }])

    .service('batchRestService', ['$http', function ($http) {
        var jobsUrl = jberetui.restApiUrl + '/jobs';
        var jobInstancesUrl = jberetui.restApiUrl + '/jobinstances';
        var jobExecutionsUrl = jberetui.restApiUrl + '/jobexecutions';

        this.getJobs = function() {
            return $http.get(jobsUrl);
        };

        this.startJob = function(jobXmlName, jobParameters) {
            return $http.post(jobsUrl + '/' + jobXmlName + '/start', jobParameters);
        };

        this.getJobInstances = function(jobName) {
            if(jobName) {
                return $http.get(jobInstancesUrl + '?jobName=' + jobName);
            }
            return $http.get(jobInstancesUrl);
        };

        this.getJobExecutions = function(running, jobName, jobInstanceId, jobExecutionId1) {
            if(running && jobName) {
                return $http.get(jobExecutionsUrl + '/running?jobName=' + jobName);
            } else if(jobExecutionId1) {
                return $http.get(jobExecutionsUrl + '?jobExecutionId1=' + jobExecutionId1 + '&jobInstanceId=' +
                    (jobInstanceId ? jobInstanceId : 0));
            } else {
                return $http.get(jobExecutionsUrl);
            }
        };

        this.getJobExecution = function(jobExecutionId) {
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

    }]);

var jberetui = {
    /**
     * URL for JBeret REST API.
     * The URL string includes hostname, port number, application context path, and REST API path.
     * Users need to modify this property for their environment.
     */
    restApiUrl: 'http://localhost:8080/restAPI/api',

    parseJobParameters: function (keyValues) {
        if (keyValues == null) {
            return null;
        }
        keyValues = keyValues.trim();
        if (keyValues.length == 0) {
            return null;
        }
        var result = {};
        var lines = keyValues.split(/\r\n|\r|\n/g);
        var x;
        for (x in lines) {
            var line = lines[x].trim();
            if (line.length == 0) {
                continue;
            }
            var pair = line.split('=');
            var key = pair[0].trim();
            result[key] = pair.length > 1 ? pair[1].trim() : '';
        }
        return result;
    },

    getIdFromUrl: function (url, tokenBeforeId) {
        var result = null;
        var tokenStartPos = url.lastIndexOf(tokenBeforeId);
        if (tokenStartPos >= 0) {
            var startOfId = tokenStartPos + tokenBeforeId.length;
            var stopPos = url.indexOf('/', startOfId + 1);
            if (stopPos < 0) {
                stopPos = url.length;
            }
            result = url.substring(startOfId, stopPos);
        }
        return result;
    },

    getColor: function (data) {
        return data == 'COMPLETED' ? 'text-success' :
            data == 'FAILED' || data == 'ABANDONED' ? 'text-danger' :
                data == 'STOPPED' || data == 'STOPPING' ? 'text-warning' :
                    'text-primary';
    },

    formatAsKeyValuePairs: function (obj) {
        var result = '';
        if (!obj) {
            return result;
        }
        for (var p in obj) {
            if(obj.hasOwnProperty(p)) {
                result = result + p + ' = ' + obj[p] + ', ';
            }
        }
        if (result.length > 1) {
            result = result.substring(0, result.length - 2);
        }
        return result;
    }
};
