'use strict';

var angular = require('angular');
var utils = require('../common/utils');

angular.module('jberetUI.jobexecutions',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobexecutions', {
            url: '/jobexecutions?jobName&running&jobExecutionId1&jobInstanceId',
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl'
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$state', '$stateParams', '$log', 'batchRestService',
        function ($scope, $state, $stateParams, $log, batchRestService) {
            var detailsLinkCell =
                '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="details({jobExecutionId: COL_FIELD, jobExecutionEntity: row.entity, jobName: grid.appScope.jobTrace.jobName, jobInstanceId: grid.appScope.jobTrace.jobInstanceId, jobExecutionId1: grid.appScope.jobTrace.jobExecutionId1, running: grid.appScope.jobTrace.running})">{{COL_FIELD}}</a></div>';

            //save job trace info to $scope for future state transition use
            $scope.jobTrace = {
                jobName: $stateParams.jobName,
                jobInstanceId: $stateParams.jobInstanceId,
                jobExecutionId1: $stateParams.jobExecutionId1,
                running: $stateParams.running
            };

            $scope.gridOptions = new utils.DefaultGridOptions(15, true, 'job-executions.csv',
                [
                    {name: 'executionId', type: 'number', cellTemplate: detailsLinkCell, headerTooltip: true},
                    {name: 'jobInstanceId', type: 'number', headerTooltip: true},
                    {name: 'jobName', cellTooltip: true, headerTooltip: true},
                    {name: 'jobParameters', cellTooltip: true, headerTooltip: true},
                    {name: 'batchStatus', headerTooltip: true},
                    {name: 'exitStatus', cellTooltip: true, headerTooltip: true},
                    {
                        name: 'createTime', cellFilter: utils.dateCellFilter, cellTooltip: false, type: 'date',
                        headerTooltip: 'Create Time ' + utils.dateFormat
                    },
                    {
                        name: 'startTime', cellFilter: utils.dateCellFilter, cellTooltip: false, type: 'date',
                        headerTooltip: 'Start Time ' + utils.dateFormat
                    },
                    {
                        name: 'lastUpdatedTime',
                        cellFilter: utils.dateCellFilter,
                        cellTooltip: false,
                        type: 'date',
                        headerTooltip: 'Last Updated Time ' + utils.dateFormat
                    },
                    {
                        name: 'endTime', cellFilter: utils.dateCellFilter, cellTooltip: false, type: 'date',
                        headerTooltip: 'End Time ' + utils.dateFormat
                    }
                ]
            );
            //rowHeight:40,
            //when cellFilter: date is used, cellTooltip shows unresolved expression, so not to show it

            //set pageTitle depending on query params
            $scope.pageTitle =
                $stateParams.running && $stateParams.jobName ? 'Running Job Executions for Job ' + $stateParams.jobName :
                $stateParams.jobInstanceId && $stateParams.jobExecutionId1 ? 'Job Executions in Job Instance ' + $stateParams.jobInstanceId :
                    'Job Executions';

            $scope.backToJobInstances = function () {
                $state.go('jobinstances', {
                    jobName: $scope.jobTrace.jobName
                });
            };

            batchRestService.getJobExecutions($stateParams.running, $stateParams.jobName, $stateParams.jobInstanceId, $stateParams.jobExecutionId1)
                .then(function (responseData) {
                    $scope.gridOptions.data = responseData.data;
                }, function (responseData) {
                    $log.debug(responseData);
                });
        }]);
