'use strict';

angular.module('jberetUI.jobexecutions',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobexecutions', {
            url: '/jobexecutions',
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl'
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$http', 'uiGridConstants', function ($scope, $http, uiGridConstants) {
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

            //when cellFilter: date is used, cellTooltip shows unresolved expression, so not to show it
            columnDefs: [
                {name: 'executionId', type: 'number', cellTemplate: urlCellTemp, headerTooltip: true,
                    sort: {direction: uiGridConstants.DESC}},
                {name: 'jobInstanceId', type: 'number', headerTooltip: true},
                {name: 'jobName', cellTooltip: true, headerTooltip: true},
                {name: 'jobParameters', cellTooltip: true, headerTooltip: true},
                {name: 'batchStatus', headerTooltip: true},
                {name: 'exitStatus', cellTooltip: true, headerTooltip: true},
                {name: 'createTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date', headerTooltip: true},
                {name: 'startTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date', headerTooltip: true},
                {name: 'lastUpdatedTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date', headerTooltip: true},
                {name: 'endTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date', headerTooltip: true}
            ]
        };

        $http.get('http://localhost:8080/restAPI/api/jobexecutions')
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });
    }]);
