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
            var urlCellTemp =
                '<div class="ngCellText" ng-class="col.colIndex()"><a ui-sref="jobexecutions({jobExecutionEntities: row.entity.jobExecutions})">{{COL_FIELD}}</a></div>';

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
                    {name: 'numberOfJobExecutions', type: 'number', cellTemplate: urlCellTemp}
                ]
            };

            var jobInstancesUrl = $stateParams.jobName ?
            'http://localhost:8080/restAPI/api/jobinstances?jobName=' + $stateParams.jobName :
                'http://localhost:8080/restAPI/api/jobinstances';
            $http.get(jobInstancesUrl)
                .then(function (responseData) {
                    $scope.gridOptions.data = responseData.data;
                }, function (responseData) {
                    console.log(responseData);
                });

        }]);
