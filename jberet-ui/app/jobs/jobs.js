'use strict';

angular.module('jberetUI.jobs',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobs', {
            url: '/jobs',
            templateUrl: 'jobs/jobs.html',
            controller: 'JobsCtrl'
        });
    }])

    .controller('JobsCtrl', ['$scope', '$http', function ($scope, $http) {
        $scope.alerts = [];
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
            if($scope.jobName) {
                var jobParams = parseJobParameters($scope.jobParameters);
                $http.post('http://localhost:8080/restAPI/api/jobs/' + $scope.jobName + '/start', jobParams).then(function (responseData) {
                    $scope.alerts.push({type: 'success',
                        msg: 'Started job: ' + $scope.jobName +
                        (jobParams == null ? '.' : ', with parameters: ' + JSON.stringify(jobParams))});
                    $scope.jobName = '';
                    $scope.jobParameters = '';
                }, function (responseData) {
                    console.log(responseData);
                    $scope.alerts.push({type: 'danger', msg: 'Failed to start job: ' + $scope.jobName});
                    $scope.jobName = '';
                    $scope.jobParameters = '';
                });
            } else {
                $scope.alerts.push({type: 'danger', msg: 'Enter a valid job XML name'});
            }
        };

        $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
        }
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
