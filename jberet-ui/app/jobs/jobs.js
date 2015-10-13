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
            minRowsToShow: 12,
            columnDefs: [
                {name: 'jobName'},
                {name: 'numberOfJobInstances'},
                {name: 'numberOfRunningJobExecutions'}
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
                $scope.jobName = '';
            }, function (responseData) {
                console.log(responseData);
                $scope.jobStartResult = 'Failed to start job: ' + $scope.jobName;
                $scope.jobName = '';
            });
        };
    }]);
