'use strict';

angular.module('jberetUI.jobexecutions',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobexecutions', {
            url: '/jobexecutions',
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl',
            params: {
                jobExecutionEntities: null
            }
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$http', '$stateParams', function ($scope, $http, $stateParams) {
        var urlCellTemp =
'<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="details({jobExecutionId: COL_FIELD, jobExecutionEntity: row.entity})">{{COL_FIELD}}</a></div>';

        $scope.gridOptions = {
            enableGridMenu: true,
            enableSelectAll: true,
            exporterCsvFilename: 'job-executions.csv',

            enableFiltering: true,
            showGridFooter: true,
            minRowsToShow: 15,
            //rowHeight:40,

            //when cellFilter: date is used, cellTooltip shows unresolved expression, so not to show it
            columnDefs: [
                {name: 'executionId', type: 'number', cellTemplate: urlCellTemp, headerTooltip: true},
                {name: 'jobInstanceId', type: 'number', headerTooltip: true},
                {name: 'jobName', cellTooltip: true, headerTooltip: true},
                {name: 'jobParameters', cellTooltip: true, headerTooltip: true},
                {name: 'batchStatus', headerTooltip: true},
                {name: 'exitStatus', cellTooltip: true, headerTooltip: true},
                {name: 'createTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date',
                    headerTooltip: 'Create Time HH:mm:ss MM-dd-yyyy'},
                {name: 'startTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date',
                    headerTooltip: 'Start Time HH:mm:ss MM-dd-yyyy'},
                {name: 'lastUpdatedTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date',
                    headerTooltip: 'Last Updated Time HH:mm:ss MM-dd-yyyy'},
                {name: 'endTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date',
                    headerTooltip: 'End Time HH:mm:ss MM-dd-yyyy'}
            ]
        };

        if($stateParams.jobExecutionEntities) {
            $scope.gridOptions.data = $stateParams.jobExecutionEntities;
        } else {
            $http.get('http://localhost:8080/restAPI/api/jobexecutions')
                .then(function (responseData) {
                    $scope.gridOptions.data = responseData.data;
                }, function (responseData) {
                    console.log(responseData);
                });
        }
    }]);
