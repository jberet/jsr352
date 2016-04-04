'use strict';

var angular = require('angular');
var utils = require('../common/utils');

angular.module('jberetUI.schedules', ['ui.router', 'ui.bootstrap', 'ngAnimate', 'ngTouch'])

    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state('schedules', {
            url: '/schedules',
            templateUrl: 'schedules/schedules.html',
            controller: 'SchedulesCtrl'
        });
    }])

    .controller('SchedulesCtrl', ['$scope', '$log', 'modalService', 'batchRestService',
        function ($scope, $log, modalService, batchRestService) {
        $scope.alerts = [];
            
        $scope.getAllSchedules = function () {
            $scope.alerts.length = 0; //clear alerts
            getSchedules();  
        };

        $scope.cancelSchedule = function (idToCancel) {
            $scope.alerts.length = 0; //clear alerts
            var modalOptions = {
                closeButtonText: 'Do Not Cancel Schedule',
                actionButtonText: 'Cancel Schedule',
                bodyText: 'Cancel Schedule ' + idToCancel + ' ?'
            };

            modalService.showModal({}, modalOptions).then(function (result) {
                if (result) {
                    batchRestService.cancelJobSchedule(idToCancel)
                        .then(function (responseData) {
                            if(responseData.data) {
                                $scope.alerts.push({
                                    type: 'success',
                                    msg: 'Cancelled job schedule: ' + idToCancel
                                });
                            } else {
                                $scope.alerts.push({
                                    type: 'danger',
                                    msg: 'Failed to cancel schedule: ' + idToCancel
                                });
                            }
                        }, function (responseData) {
                            $log.debug(responseData);
                            $scope.alerts.push({
                                type: 'danger',
                                msg: 'Failed to cancel schedule: ' + idToCancel
                            });
                        });
                }
            });
        };

        $scope.closeAlert = function (index) {
            $scope.alerts.splice(index, 1);
        };

        function getSchedules() {
            batchRestService.getJobSchedules().then(function (responseData) {
                $scope.schedules = responseData.data;
            }, function (responseData) {
                $log.debug(responseData);
            });
        }

        getSchedules();
    }]);
