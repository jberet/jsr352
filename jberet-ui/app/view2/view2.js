'use strict';

angular.module('myApp.view2', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view2', {
            templateUrl: 'view2/view2.html',
            controller: 'View2Ctrl'
        });
    }])

    .controller('View2Ctrl', ['$scope', '$http', function ($scope, $http) {
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


        $http.get('http://localhost:8080/restAPI/api/jobexecutions', null)
            .then(function (responseData) {
            console.log(responseData.data);
            $scope.gridOptions.data = [
                responseData.data
            ];

        }, function (responseData) {
            alert(responseData.data)
        });

    }]);