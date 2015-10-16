'use strict';

angular.module('jberetUI.jobinstances',
    ['ui.router', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobinstances', {
            url: '/jobinstances',
            templateUrl: 'jobinstances/jobinstances.html',
            controller: 'JobInstancesCtrl'
        });
    }])

    .controller('JobInstancesCtrl', ['$scope', '$http', function ($scope, $http) {
        $scope.gridOptions = {
            enableGridMenu: true,
            enableSelectAll: true,
            exporterCsvFilename: 'job-instances.csv',

            enableFiltering: true,
            showGridFooter: true,
            minRowsToShow: 15,
            columnDefs: [
                {name: 'instanceId', type: 'number'},
                {name: 'jobName'},
                {name: 'numberOfJobExecutions', type: 'number'}
            ],

        };


        $http.get('http://localhost:8080/restAPI/api/jobinstances')
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });

    }]);
