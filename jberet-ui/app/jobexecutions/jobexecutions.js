'use strict';

angular.module('jberetUI.jobexecutions', ['ngRoute', 'ui.grid'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/jobexecutions', {
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl'
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$http', function ($scope, $http) {
        var cellTemp =
            '<div ng-class="col.colIndex()"><div>{{grid.getCellValue(row, col) | date:"yyyy-MM-dd HH:mm:ss"}}</div></div>';

        $scope.gridOptions = {
            columnDefs: [
                {field: 'executionId'},
                {field: 'jobName'},
                {field: 'jobParameters'},
                {field: 'batchStatus'},
                {field: 'exitStatus'},
                {field: 'createTime', cellTemplate: cellTemp},
                {field: 'startTime', cellTemplate: cellTemp},
                {field: 'lastUpdatedTime', cellTemplate: cellTemp},
                {field: 'endTime', cellTemplate: cellTemp}
            ]
        };

        $http.get('http://localhost:8080/restAPI/api/jobexecutions')
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });
    }]);
