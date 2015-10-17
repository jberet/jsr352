'use strict';

angular.module('jberetUI.details',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('details', {
            url: '/jobexecutions/:jobExecutionId',
            templateUrl: 'details/details.html',
            controller: 'DetailsCtrl',
            params: {jobExecutionEntity: null}
        });
    }])

    .controller('DetailsCtrl', ['$scope', '$http', '$stateParams', '$location',
        function ($scope, $http, $stateParams, $location) {
            $scope.alerts = [];
            var dateCellTemp =
                '<div ng-class="col.colIndex()">{{COL_FIELD | date:"yyyy-MM-dd HH:mm:ss"}}</div>';

            $scope.gridOptions = {
                enableGridMenu: true,
                enableSelectAll: true,
                exporterCsvFilename: 'step-execution.csv',

                enableFiltering: true,
                //showGridFooter: true,
                minRowsToShow: 5,
                rowHeight: 40,

                columnDefs: [
                    {name: 'stepExecutionId', type: 'number'},
                    {name: 'stepName', cellTooltip: true},
                    {name: 'persistentUserData', cellTooltip: true},
                    {name: 'batchStatus'},
                    {name: 'exitStatus', cellTooltip: true},
                    {name: 'startTime', cellTemplate: dateCellTemp, cellTooltip: true, type: 'date'},
                    {name: 'endTime', cellTemplate: dateCellTemp, cellTooltip: true, type: 'date'},
                    {name: 'metrics', cellTooltip: true}
                ]
            };

            var handleJobExecutionEntity = function() {
                $http.get($scope.jobExecutionEntity.href + '/stepexecutions')
                    .then(function (responseData) {
                        $scope.gridOptions.data = responseData.data;
                    }, function (responseData) {
                        console.log(responseData);
                    });
                var batchStatus = $scope.jobExecutionEntity.batchStatus;
                $scope.stopDisabled = batchStatus != 'STARTING' && batchStatus != 'STARTED';
                $scope.restartDisabled = batchStatus != 'STOPPED' && batchStatus != 'FAILED';
                $scope.abandonDisabled = batchStatus == 'STARTING' || batchStatus == 'STARTED' || batchStatus == 'STOPPING';
            };

            if ($stateParams.jobExecutionEntity) {
                $scope.jobExecutionEntity = $stateParams.jobExecutionEntity;
                handleJobExecutionEntity();
            } else {
                var paths = $location.path().split('/');
                var length = paths.length;
                var idPart = length == 1 ? paths[0] :
                    paths[length - 1].length == 0 ? paths[length - 2] : paths[length - 1];
                $http.get('http://localhost:8080/restAPI/api/jobexecutions/' + idPart)
                    .then(function (responseData) {
                        $scope.jobExecutionEntity = responseData.data;
                        handleJobExecutionEntity();
                    }, function (responseData) {
                        console.log(responseData);
                        $scope.stopDisabled = $scope.restartDisabled = $scope.abandonDisabled = true;
                        $scope.alerts.push({type: 'danger', msg: 'Failed to get job execution with id ' + idPart});
                    });
            }

            $scope.stopJobExecution = function () {
                var idToStop = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0; //clear alerts
                $http.post('http://localhost:8080/restAPI/api/jobexecutions/' + idToStop + '/stop', null)
                    .then(function (responseData) {
                        $scope.jobExecutionEntity = responseData.data;
                        handleJobExecutionEntity();
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
            };

            $scope.restartJobExecution = function() {
                var idToRestart = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0; //clear alerts
                //TODO add restart params
                $http.post('http://localhost:8080/restAPI/api/jobexecutions/' + idToRestart + '/restart', null)
                    .then(function (responseData) {
                        $scope.restartJobExecutionEntity = responseData.data;
                        $scope.restartDisabled = true;
                        $scope.alerts.push({
                            type: 'success',
                            msg: 'Restarted job execution ' + idToRestart +
                                ', and the new execution is ' + $scope.restartJobExecutionEntity.executionId
                        });
                    }, function (responseData) {
                        console.log(responseData);
                        $scope.alerts.push({
                            type: 'danger',
                            msg: 'Failed to restart job execution ' + idToRestart
                        });
                    });
            };

            $scope.abandonJobExecution = function() {
                var idToAbandon = $scope.jobExecutionEntity.executionId;
                $scope.alerts.length = 0; //clear alerts
                $http.post('http://localhost:8080/restAPI/api/jobexecutions/' + idToAbandon + '/abandon', null)
                    .then(function () {
                        $scope.jobExecutionEntity.batchStatus = 'ABANDONED';
                        $scope.abandonDisabled = true;
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
            };

            $scope.closeAlert = function (index) {
                $scope.alerts.splice(index, 1);
            };
        }]);
