'use strict';

angular.module('jberetUI.jobs', ['ngRoute', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/jobs', {
            templateUrl: 'jobs/jobs.html',
            controller: 'JobsCtrl'
        });
    }])

    .controller('JobsCtrl', ['$scope', '$http', function ($scope, $http) {
        $scope.jobStartResult = "";
        $scope.gridOptions = {
            enableGridMenu: true,
            enableSelectAll: true,
            exporterCsvFilename: 'jobs.csv',

            enableFiltering: true,
            showGridFooter: true,
            minRowsToShow: 8,
            columnDefs: [
                {name: 'jobName'},
                {name: 'numberOfJobInstances', type: 'number'},
                {name: 'numberOfRunningJobExecutions', type: 'number'}
            ]
        };

        $http.get('http://localhost:8080/restAPI/api/jobs/').then(function (responseData) {
            $scope.gridOptions.data = responseData.data;
        }, function (responseData) {
            console.log(responseData);
        });


        $scope.startJob = function () {
            var jobParams = parseJobParameters($scope.jobParameters);
            console.log(jobParams);
            $http.post('http://localhost:8080/restAPI/api/jobs/' + $scope.jobName + '/start', jobParams).then(function (responseData) {
                $scope.jobStartResult = 'Started job: ' + $scope.jobName +
                    (jobParams == null ? '.' : ', with parameters: ' + JSON.stringify(jobParams));
                $scope.jobName = '';
                $scope.jobParameters = '';
            }, function (responseData) {
                console.log(responseData);
                $scope.jobStartResult = 'Failed to start job: ' + $scope.jobName;
                $scope.jobName = '';
                $scope.jobParameters = '';
            });
        };
    }]);

function parseJobParameters(keyValues) {
    if(keyValues == null) {
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
        if(line.length == 0) {
            continue;
        }
        var pair = line.split('=');
        var key = pair[0].trim();
        result[key] = pair.length > 1 ? pair[1].trim() : '';
    }
    return result;
}
