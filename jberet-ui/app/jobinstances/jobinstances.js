'use strict';

angular.module('jberetUI.jobinstances',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobinstances', {
            url: '/jobinstances?jobName',
            templateUrl: 'jobinstances/jobinstances.html',
            controller: 'JobInstancesCtrl'
        });
    }])

    .controller('JobInstancesCtrl', ['$scope', '$http', '$stateParams',
        function ($scope, $http, $stateParams) {
            var jobExecutionsLinkCell =
                '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="jobexecutions({jobName: row.entity.jobName, jobInstanceId: row.entity.instanceId, jobExecutionId1: row.entity.latestJobExecutionId})">{{COL_FIELD}}</a></div>';

            var detailsLinkCell =
                '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="details({jobExecutionId: row.entity.latestJobExecutionId, jobName: row.entity.jobName, jobInstanceId: row.entity.instanceId, jobExecutionId1: row.entity.latestJobExecutionId})">{{COL_FIELD}}</a></div>';

            $scope.gridOptions = {
                enableGridMenu: true,
                enableSelectAll: true,
                exporterCsvFilename: 'job-instances.csv',

                enableFiltering: true,
                showGridFooter: true,
                minRowsToShow: 15,
                columnDefs: [
                    {name: 'instanceId', type: 'number'},
                    {name: 'jobName'},
                    {name: 'numberOfJobExecutions', type: 'number', cellTemplate: jobExecutionsLinkCell},
                    {name: 'latestJobExecutionId', type: 'number', cellTemplate: detailsLinkCell}
                ]
            };

            var jobInstancesUrl = null;
            if($stateParams.jobName) {
                jobInstancesUrl = 'http://localhost:8080/restAPI/api/jobinstances?jobName=' + $stateParams.jobName;
            } else {
                jobInstancesUrl = 'http://localhost:8080/restAPI/api/jobinstances';
            }

            $scope.pageTitle = $stateParams.jobName ? 'Job Instances for Job ' + $stateParams.jobName : 'Job Instances';

            $http.get(jobInstancesUrl)
                .then(function (responseData) {
                    $scope.gridOptions.data = responseData.data;
                }, function (responseData) {
                    console.log(responseData);
                });

        }]);
