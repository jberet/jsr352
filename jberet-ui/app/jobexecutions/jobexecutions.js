'use strict';

angular.module('jberetUI.jobexecutions',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobexecutions', {
            url: '/jobexecutions?jobName&running&jobExecutionId1&jobInstanceId',
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl'
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$http', '$state', '$stateParams', function ($scope, $http, $state, $stateParams) {
        var detailsLinkCell =
'<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="details({jobExecutionId: COL_FIELD, jobExecutionEntity: row.entity, jobName: grid.appScope.jobTrace.jobName, jobInstanceId: grid.appScope.jobTrace.jobInstanceId, jobExecutionId1: grid.appScope.jobTrace.jobExecutionId1, running: grid.appScope.jobTrace.running})">{{COL_FIELD}}</a></div>';

        //save job trace info to $scope for future state transition use
        $scope.jobTrace = {
            jobName: $stateParams.jobName,
            jobInstanceId: $stateParams.jobInstanceId,
            jobExecutionId1: $stateParams.jobExecutionId1,
            running: $stateParams.running
        };

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
                {name: 'executionId', type: 'number', cellTemplate: detailsLinkCell, headerTooltip: true},
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

        var getJobInstancesUrl = function (params) {
            var url = 'http://localhost:8080/restAPI/api/jobexecutions';
            if(params.running && params.jobName) {
                url = url + '/running?jobName=' + params.jobName;
            } else if(params.jobExecutionId1) {
                url = url + '?jobExecutionId1=' + params.jobExecutionId1 + '&jobInstanceId=' +
                    (params.jobInstanceId ? params.jobInstanceId : 0);
            }
            return url;
        };

        //set pageTitle depending on query params
        $scope.pageTitle = $stateParams.jobInstanceId && $stateParams.jobExecutionId1 ? 'Job Executions in Job Instance ' + $stateParams.jobInstanceId :
                            $stateParams.running && $stateParams.jobName ? 'Running Job Executions for Job ' + $stateParams.jobName :
                                'Job Executions';

        $http.get(getJobInstancesUrl($stateParams))
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });

        $scope.backToJobInstances = function () {
            $state.go('jobinstances', {
                jobName: $scope.jobTrace.jobName
            });
        };
    }]);
