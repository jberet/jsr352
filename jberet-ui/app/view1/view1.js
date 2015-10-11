'use strict';

angular.module('myApp.view1', ['ngRoute', 'ui.grid'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view1', {
            templateUrl: 'view1/view1.html',
            controller: 'View1Ctrl'
        });
    }])

    .controller('View1Ctrl', ['$scope', '$http', function ($scope, $http) {
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

        $scope.startJob = function () {
            console.log('starting job:' + $scope.jobName);
            $http.post('http://localhost:8080/restAPI/api/jobs/' + $scope.jobName + '/start', null).then(function (responseData) {
                console.log(responseData.data);
                $scope.gridOptions.data = [
                    responseData.data
                ];

            }, function (responseData) {
                console.log(responseData)
            })
        };
    }]);