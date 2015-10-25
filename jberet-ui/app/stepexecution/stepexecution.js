'use strict';

angular.module('jberetUI.stepexecution',
    ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter', 'dangle'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('stepexecution', {
            url: '/jobexecutions/:jobExecutionId/stepexecutions/:stepExecutionId',
            templateUrl: 'stepexecution/stepexecution.html',
            controller: 'StepExecutionCtrl',
            params: {stepExecutionEntity: null,
                    jobExecutionEntity: null,
                    jobTrace: null
            }
        });
    }])

    .controller('StepExecutionCtrl', ['$scope', '$http', '$stateParams', '$state', '$location',
        function ($scope, $http, $stateParams, $state, $location) {
            $scope.alerts = [];

            var createChartData = function () {
                $scope.chartData = {
                    terms: []

                    //terms: [{
                    //    'term': 'foo',
                    //    'count': 2
                    //}, {
                    //    'term': 'baz',
                    //    'count': 1
                    //}]
                };

                /**
                 * 0    READ_COUNT
                 * 1    READ_SKIP_COUNT
                 *
                 * 2    FILTER_COUNT
                 * 3    PROCESS_SKIP_COUNT
                 *
                 * 4    WRITE_COUNT
                 * 5    WRITE_SKIP_COUNT
                 *
                 * 6    COMMIT_COUNT
                 * 7    ROLLBACK_COUNT
                 */

                var i;
                var metrics = $scope.stepExecutionEntity.metrics;
                for(i = 0; i < metrics.length; i++) {
                    var m = metrics[i];
                    if(m.type == 'READ_COUNT') {
                        $scope.chartData.terms[0] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'READ_SKIP_COUNT') {
                        $scope.chartData.terms[1] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'FILTER_COUNT') {
                        $scope.chartData.terms[2] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'PROCESS_SKIP_COUNT') {
                        $scope.chartData.terms[3] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'WRITE_COUNT') {
                        $scope.chartData.terms[4] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'WRITE_SKIP_COUNT') {
                        $scope.chartData.terms[5] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'COMMIT_COUNT') {
                        $scope.chartData.terms[6] = {'term': m.type, 'count': m.value};
                    } else if(m.type == 'ROLLBACK_COUNT') {
                        $scope.chartData.terms[7] = {'term': m.type, 'count': m.value};
                    }
                }
            };

            var getStepExecution = function (jobExecutionId, stepExecutionId) {
                $http.get('http://localhost:8080/restAPI/api/jobexecutions/' + jobExecutionId + '/stepexecutions/' + stepExecutionId)
                    .then(function (responseData) {
                        $scope.stepExecutionEntity = responseData.data;
                        createChartData();
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
                createChartData();
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
                var transitionParams = $stateParams.jobTrace ?
                {
                    jobExecutionId: jobExecutionId,
                    jobExecutionEntity : $scope.jobExecutionEntity,

                    jobName: $stateParams.jobTrace.jobName,
                    jobInstanceId: $stateParams.jobTrace.jobInstanceId,
                    jobExecutionId1: $stateParams.jobTrace.jobExecutionId1,
                    running: $stateParams.jobTrace.running
                } : {
                    jobExecutionId: jobExecutionId,
                    jobExecutionEntity : $scope.jobExecutionEntity
                };
                $state.go('details', transitionParams);
            };

            $scope.closeAlert = function (index) {
                $scope.alerts.splice(index, 1);
            };

            $scope.getColor = jberetui.getColor;

        }]);
