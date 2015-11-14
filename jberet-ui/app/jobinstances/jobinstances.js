'use strict';

var angular = require('angular');
var utils = require('../common/utils');

angular.module('jberetUI.jobinstances',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobinstances', {
            url: '/jobinstances?jobName',
            templateUrl: 'jobinstances/jobinstances.html',
            controller: 'JobInstancesCtrl'
        });
    }])

    .controller('JobInstancesCtrl', ['$scope', '$stateParams', '$log', 'batchRestService',
        function ($scope, $stateParams, $log, batchRestService) {
            var jobExecutionsLinkCell =
                '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="jobexecutions({jobName: row.entity.jobName, jobInstanceId: row.entity.instanceId, jobExecutionId1: row.entity.latestJobExecutionId})">{{COL_FIELD}}</a></div>';

            var detailsLinkCell =
                '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="details({jobExecutionId: row.entity.latestJobExecutionId, jobName: row.entity.jobName, jobInstanceId: row.entity.instanceId, jobExecutionId1: row.entity.latestJobExecutionId})">{{COL_FIELD}}</a></div>';

            $scope.gridOptions = new utils.DefaultGridOptions(15, true, 'job-instances.csv',
                [
                    {name: 'instanceId', type: 'number'},
                    {name: 'jobName'},
                    {name: 'numberOfJobExecutions', type: 'number', cellTemplate: jobExecutionsLinkCell},
                    {name: 'latestJobExecutionId', type: 'number', cellTemplate: detailsLinkCell}
                ]
            );

            $scope.pageTitle = $stateParams.jobName ? 'Job Instances for Job ' + $stateParams.jobName : 'Job Instances';

            batchRestService.getJobInstances($stateParams.jobName).then(function (responseData) {
                    $scope.gridOptions.data = responseData.data;
                }, function (responseData) {
                    $log.debug(responseData);
                });

        }]);
