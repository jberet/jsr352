'use strict';

angular.module('jberetUI.jobinstances', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/jobinstances', {
            templateUrl: 'jobinstances/jobinstances.html',
            controller: 'JobInstancesCtrl'
        });
    }])

    .controller('JobInstancesCtrl', ['$scope', '$http', function ($scope, $http) {
        $scope.gridOptions = {
            columnDefs: [
                {field: 'instanceId'},
                {field: 'jobName'},
                {field: 'jobExecutions'}
            ]
        };


        $http.get('http://localhost:8080/restAPI/api/jobinstances')
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });

    }]);
