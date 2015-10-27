'use strict';

angular.module('jberetUI.jobs',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('jobs', {
            url: '/jobs',
            templateUrl: 'jobs/jobs.html',
            controller: 'JobsCtrl'
        });
    }])

    .directive('customOnChange', function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var onChangeFunc = scope.$eval(attrs.customOnChange);
                element.bind('change', onChangeFunc);
            }
        };
    })

    .controller('JobsCtrl', ['$scope', 'batchRestService', function ($scope, batchRestService) {
        var jobInstancesLinkCell =
            '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="jobinstances({jobName: row.entity.jobName})">{{COL_FIELD}}</a></div>';

        var jobExecutionsLinkCell =
            '<div class="ngCellText" ng-class="col.colIndex()"><a ng-class="grid.appScope.getLinkActiveClass(COL_FIELD)" ui-sref="jobexecutions({jobName: row.entity.jobName, running: true})">{{COL_FIELD}}</a></div>';

        $scope.alerts = [];
        $scope.gridOptions = {
            enableGridMenu: true,
            enableSelectAll: true,
            exporterCsvFilename: 'jobs.csv',

            enableFiltering: true,
            showGridFooter: true,
            minRowsToShow: 8,
            columnDefs: [
                {name: 'jobName'},
                {name: 'numberOfJobInstances', type: 'number', cellTemplate: jobInstancesLinkCell},
                {name: 'numberOfRunningJobExecutions', type: 'number', cellTemplate: jobExecutionsLinkCell}
            ]
        };

        $scope.startJob = function () {
            $scope.alerts.length = 0; //clear alerts
            $scope.stateTransitionParams = null;
            if ($scope.jobName) {
                var jobParams = jberetui.parseJobParameters($scope.jobParameters);
                batchRestService.startJob($scope.jobName, jobParams).then(function (responseData) {
                    $scope.jobExecutionEntity = responseData.data;
                    $scope.stateTransitionParams = {jobExecutionId: $scope.jobExecutionEntity.executionId,
                                                jobExecutionEntity: $scope.jobExecutionEntity,
                                                jobName: $scope.jobName,
                                                jobInstanceId: $scope.jobExecutionEntity.jobInstanceId,
                                                jobExecutionId1: $scope.jobExecutionEntity.executionId
                    };
                    $scope.alerts.push({
                        type: 'success',
                        msg: 'Started job: ' + $scope.jobName +
                        ((!jobParams) ? '.' : ', with parameters: ' + jberetui.formatAsKeyValuePairs(jobParams) + '.')
                    });

                    getRecentJobs();
                    resetFields();
                }, function (responseData) {
                    console.log(responseData);
                    $scope.alerts.push({
                        type: 'danger',
                        msg: 'Failed to start job: ' + $scope.jobName + '.'
                    });
                    resetFields();
                });
            } else {
                $scope.alerts.push({type: 'danger', msg: 'Enter a valid job XML name.'});
            }
        };

        $scope.closeAlert = function (index) {
            $scope.alerts.splice(index, 1);
        };

        $scope.getLinkActiveClass = function(value) {
            return value > 0 ? '' : 'not-active';
        };

        $scope.uploadFile = function(event){
            var file = event.target.files[0];
            var reader = new FileReader();
            reader.onload = function(){
                $scope.$apply(function () {
                    $scope.jobParameters = reader.result;
                });
            };
            reader.readAsText(file);
        };

        function getRecentJobs() {
            batchRestService.getJobs().then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });
        }

        function resetFields() {
            $scope.jobName = '';
            $scope.jobParameters = '';
            document.getElementById('jobParametersImport').value = null;
        }

        getRecentJobs();
    }]);
