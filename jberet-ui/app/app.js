'use strict';

// Declare app level module which depends on views, and components
angular.module('jberetUI',
    ['ui.router', 'jberetUI.jobs', 'jberetUI.jobinstances', 'jberetUI.jobexecutions', 'jberetUI.details', 'jberetUI.version']).

    config(['$urlRouterProvider', function ($urlRouterProvider) {
            $urlRouterProvider.otherwise('/jobs');
        }]);
