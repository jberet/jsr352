'use strict';

angular.module('jberetUI.details',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter', 'modal-module'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('details', {
            url: '/jobexecutions/:jobExecutionId',
            templateUrl: 'details/details.html',
            controller: 'DetailsCtrl',
            params: {
                jobExecutionEntity: null,
                jobName: null,
                jobInstanceId: null,
                jobExecutionId1: null,
                running: null
            }
        });
    }])

    .controller('DetailsCtrl', ['$scope', '$http', '$stateParams', '$state', '$location', 'modalService',
        function ($scope, $http, $stateParams, $state, $location, modalService) {
            var stepExecutionLinkCell =
'<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="stepexecution({stepExecutionId: COL_FIELD, stepExecutionEntity: row.entity, jobExecutionEntity: grid.appScope.jobExecutionEntity, jobExecutionId: grid.appScope.jobExecutionEntity.executionId, jobTrace: grid.appScope.jobTrace})">{{COL_FIELD}}</a></div>';

            $scope.alerts = [];
            $scope.jobTrace = $stateParams.jobTrace || {
                jobName: $stateParams.jobName,
                jobInstanceId: $stateParams.jobInstanceId,
                jobExecutionId1: $stateParams.jobExecutionId1,
                running: $stateParams.running
            };

            $scope.gridOptions = {
                enableGridMenu: true,
                enableSelectAll: true,
                exporterCsvFilename: 'step-execution.csv',

                enableFiltering: true,
                //showGridFooter: true,
                minRowsToShow: 5,
                //rowHeight: 30,

                //when cellFilter: date is used, cellTooltip shows unresolved expression, so not to show it
                columnDefs: [
                    {name: 'stepExecutionId', type: 'number', cellTemplate: stepExecutionLinkCell, headerTooltip: true},
                    {name: 'stepName', cellTooltip: true, headerTooltip: true},
                    {name: 'batchStatus', headerTooltip: true},
                    {name: 'exitStatus', cellTooltip: true, headerTooltip: true},
                    {name: 'startTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date',
                        headerTooltip: 'Start Time HH:mm:ss MM-dd-yyyy'},
                    {name: 'endTime', cellFilter: 'date:"HH:mm:ss MM-dd-yyyy "', cellTooltip: false, type: 'date',
                        headerTooltip: 'End Time HH:mm:ss MM-dd-yyyy'},
                    {name: 'metrics', cellTooltip: true, headerTooltip: true}
                ]
            };

            var handleJobExecutionEntity = function () {
                $http.get($scope.jobExecutionEntity.href + '/stepexecutions')
                    .then(function (responseData) {
                        $scope.gridOptions.data = responseData.data;
                    }, function (responseData) {
                        console.log(responseData);
                        $scope.alerts.push({
                            type: 'danger',
                            msg: 'Failed to get step executions for job execution ' + $scope.jobExecutionEntity.executionId
                        });
                    });
                var batchStatus = $scope.jobExecutionEntity.batchStatus;
                $scope.stopDisabled = batchStatus != 'STARTING' && batchStatus != 'STARTED';
                $scope.restartDisabled = batchStatus != 'STOPPED' && batchStatus != 'FAILED';
                $scope.abandonDisabled = batchStatus == 'STARTING' || batchStatus == 'STARTED'
                                        || batchStatus == 'STOPPING' || batchStatus == 'ABANDONED';
            };

            var getJobExecution = function (idPart) {
                $http.get('http://localhost:8080/restAPI/api/jobexecutions/' + idPart)
                    .then(function (responseData) {
                        $scope.jobExecutionEntity = responseData.data;
                        handleJobExecutionEntity();
                    }, function (responseData) {
                        console.log(responseData);
                        $scope.stopDisabled = $scope.restartDisabled = $scope.abandonDisabled = true;
                        $scope.alerts.push({type: 'danger', msg: 'Failed to get job execution with id ' + idPart});
                    });
            };

            if ($stateParams.jobExecutionEntity) {
                $scope.jobExecutionEntity = $stateParams.jobExecutionEntity;
                handleJobExecutionEntity();
            } else {
                getJobExecution(jberetui.getIdFromUrl($location.path(), '/jobexecutions/'));
            }

            $scope.stopJobExecution = function () {
                var idToStop = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0; //clear alerts

                var modalOptions = {
                    bodyText: 'Stop job execution ' + idToStop + '?',
                    actionButtonText: 'Stop Job Execution'
                };

                modalService.showModal({}, modalOptions).then(function (result) {
                    if (result) {
                        $http.post('http://localhost:8080/restAPI/api/jobexecutions/' + idToStop + '/stop', null)
                            .then(function () {
                                $scope.jobExecutionEntity.batchStatus = 'STOPPING';
                                $scope.alerts.push({
                                    type: 'success',
                                    msg: 'Submitted stop request for job execution ' + idToStop
                                });
                            }, function (responseData) {
                                console.log(responseData);
                                $scope.alerts.push({
                                    type: 'danger',
                                    msg: 'Failed to stop job execution ' + idToStop
                                });
                            });
                    }
                });
            };

            $scope.restartJobExecution = function () {
                var idToRestart = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0; //clear alerts
                $scope.stateTransitionParams = null;

                var modalOptions = {
                    bodyText: 'Restart job execution ' + idToRestart + '?',
                    actionButtonText: 'Restart Job Execution'
                };

                modalService.showModal({}, modalOptions).then(function (result) {
                    if (result) {
                        var jobParams = jberetui.parseJobParameters($scope.jobParameters);
                        $http.post('http://localhost:8080/restAPI/api/jobexecutions/' + idToRestart + '/restart', jobParams)
                            .then(function (responseData) {
                                $scope.restartJobExecutionEntity = responseData.data;
                                $scope.restartDisabled = true;
                                $scope.stateTransitionParams = {
                                    jobExecutionId: $scope.restartJobExecutionEntity.executionId,
                                    jobExecutionEntity: $scope.restartJobExecutionEntity,
                                    jobName: $scope.restartJobExecutionEntity.jobName,
                                    jobInstanceId: $scope.restartJobExecutionEntity.jobInstanceId,
                                    jobExecutionId1: $scope.restartJobExecutionEntity.executionId
                                };
                                $scope.alerts.push({
                                    type: 'success',
                                    msg: 'Restarted job execution ' + idToRestart +
                                    (jobParams == null ? '.' : ', with additional parameters: ' + JSON.stringify(jobParams) + '.')
                                });
                                $scope.jobParameters = '';
                            }, function (responseData) {
                                console.log(responseData);
                                $scope.alerts.push({
                                    type: 'danger',
                                    msg: 'Failed to restart job execution ' + idToRestart + '.'
                                });
                            });
                    }
                });
            };

            $scope.abandonJobExecution = function () {
                var idToAbandon = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0; //clear alerts

                var modalOptions = {
                    bodyText: 'Abandon job execution ' + idToAbandon + '?',
                    actionButtonText: 'Abandon Job Execution'
                };

                modalService.showModal({}, modalOptions).then(function (result) {
                    if (result) {
                        $http.post('http://localhost:8080/restAPI/api/jobexecutions/' + idToAbandon + '/abandon', null)
                            .then(function () {
                                $scope.jobExecutionEntity.batchStatus = 'ABANDONED';
                                $scope.abandonDisabled = $scope.stopDisabled = $scope.restartDisabled = true;
                                $scope.alerts.push({
                                    type: 'success',
                                    msg: 'Abandoned job execution ' + idToAbandon
                                });
                            }, function (responseData) {
                                console.log(responseData);
                                $scope.alerts.push({
                                    type: 'danger',
                                    msg: 'Failed to abandon job execution ' + idToAbandon
                                });
                            });
                    }
                })
            };

            $scope.refreshJobExecution = function () {
                var idToRefresh = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0;  //clear alerts
                getJobExecution(idToRefresh);
            };

            $scope.backToJobExecutions = function () {
                $state.go('jobexecutions', {
                    jobName: $scope.jobTrace.jobName,
                    jobInstanceId: $scope.jobTrace.jobInstanceId,
                    jobExecutionId1: $scope.jobTrace.jobExecutionId1,
                    running: $scope.jobTrace.running
                });
            };

            $scope.closeAlert = function (index) {
                $scope.alerts.splice(index, 1);
            };

            $scope.getColor = function (data) {
                return data == 'COMPLETED' ? 'text-success' :
                    data == 'FAILED' || data == 'ABANDONED' ? 'text-danger' :
                        data == 'STOPPED' || data == 'STOPPING' ? 'text-warning' :
                            'text-primary';
            };
        }]);
