'use strict';

angular.module('jberetUI.jobexecutions',
    ['ui.router', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobexecutions', {
            url: '/jobexecutions',
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl'
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$http', function ($scope, $http) {
        var dateCellTemp =
        '<div ng-class="col.colIndex()">{{COL_FIELD | date:"yyyy-MM-dd HH:mm:ss"}}</div>';
        var urlCellTemp =
'<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="details({jobExecutionId: COL_FIELD, jobExecutionEntity: row.entity})">{{COL_FIELD}}</a></div>';

        $scope.gridOptions = {
            enableGridMenu: true,
            enableSelectAll: true,
            exporterCsvFilename: 'job-executions.csv',

            enableFiltering: true,
            showGridFooter: true,
            minRowsToShow: 10,
            rowHeight:40,
            columnDefs: [
                {name: 'executionId', type: 'number', cellTemplate: urlCellTemp},
                {name: 'jobName', cellTooltip: true},
                {name: 'jobParameters', cellTooltip: true},
                {name: 'batchStatus'},
                {name: 'exitStatus', cellTooltip: true},
                {name: 'createTime', cellTemplate: dateCellTemp, cellTooltip: true, type: 'date'},
                {name: 'startTime', cellTemplate: dateCellTemp, cellTooltip: true, type: 'date'},
                {name: 'lastUpdatedTime', cellTemplate: dateCellTemp, cellTooltip: true, type: 'date'},
                {name: 'endTime', cellTemplate: dateCellTemp, cellTooltip: true, type: 'date'}
            ]
        };

        $http.get('http://localhost:8080/restAPI/api/jobexecutions')
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });
    }]);
