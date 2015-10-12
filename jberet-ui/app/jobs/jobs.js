'use strict';

angular.module('jberetUI.jobs', ['ngRoute', 'ui.grid'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/jobs', {
            templateUrl: 'jobs/jobs.html',
            controller: 'JobsCtrl'
        });
    }])

    .controller('JobsCtrl', ['$scope', '$http', function ($scope, $http) {
        $scope.jobStartResult = "";
        $scope.gridOptions = {
            columnDefs: [
                {field: 'jobName'},
                {field: 'numberOfJobInstances'},
                {field: 'numberOfRunningJobExecutions'}
            ]
        };

        $http.get('http://localhost:8080/restAPI/api/jobs/').then(function (responseData) {
            $scope.gridOptions.data = responseData.data;
        }, function (responseData) {
            console.log(responseData);
        });


        $scope.startJob = function () {
            $http.post('http://localhost:8080/restAPI/api/jobs/' + $scope.jobName + '/start', null).then(function (responseData) {
                $scope.jobStartResult = 'Started job: ' + $scope.jobName;
            }, function (responseData) {
                console.log(responseData);
                $scope.jobStartResult = 'Failed to start job: ' + $scope.jobName;
            });
        };
    }]);
