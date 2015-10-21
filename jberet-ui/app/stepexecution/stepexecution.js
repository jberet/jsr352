'use strict';

angular.module('jberetUI.stepexecution',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('stepexecution', {
            url: '/jobexecutions/:jobExecutionId/stepexecutions/:stepExecutionId',
            templateUrl: 'stepexecution/stepexecution.html',
            controller: 'StepExecutionCtrl',
            params: {stepExecutionEntity: null,
                    jobExecutionEntity: null}
        });
    }])

    .controller('StepExecutionCtrl', ['$scope', '$http', '$stateParams', '$state', '$location',
        function ($scope, $http, $stateParams, $state, $location) {
            $scope.alerts = [];

            var getStepExecution = function (jobExecutionId, stepExecutionId) {
                $http.get('http://localhost:8080/restAPI/api/jobexecutions/' + jobExecutionId + '/stepexecutions/' + stepExecutionId)
                    .then(function (responseData) {
                        $scope.stepExecutionEntity = responseData.data;
                    }, function (responseData) {
                        console.log(responseData);
                        $scope.alerts.push({
                            type: 'danger',
                            msg: 'Failed to get step execution with job execution id ' + jobExecutionId +
                                    ', step execution id ' + stepExecutionId
                        });
                    });
            };

            if ($stateParams.stepExecutionEntity) {
                $scope.stepExecutionEntity = $stateParams.stepExecutionEntity;
            } else {
                var url = $location.path();
                getStepExecution(jberetui.getIdFromUrl(url, '/jobexecutions/'), jberetui.getIdFromUrl(url, '/stepexecutions/'));
            }

            if ($stateParams.jobExecutionEntity) {
                $scope.jobExecutionEntity = $stateParams.jobExecutionEntity;
            }

            $scope.refreshStepExecution = function() {
                $scope.alerts.length = 0;  //clear alerts
                getStepExecution($scope.jobExecutionEntity.executionId, $scope.stepExecutionEntity.stepExecutionId);
            };

            $scope.backToJobExecution = function() {
                var jobExecutionId = $scope.jobExecutionEntity ? $scope.jobExecutionEntity.executionId :
                    jberetui.getIdFromUrl($location.path(), '/jobexecutions/');
                $state.go('details', {
                    jobExecutionId: jobExecutionId,
                    jobExecutionEntity : $scope.jobExecutionEntity
                });
            };

            $scope.closeAlert = function (index) {
                $scope.alerts.splice(index, 1);
            };

            $scope.getColor = function (data) {
                return data == 'COMPLETED' ? 'text-success' :
                        data == 'FAILED' || data == 'ABANDONED'? 'text-danger' :
                        data == 'STOPPED' || data == 'STOPPING' ? 'text-warning' :
                                'text-primary';
            };
        }]);
