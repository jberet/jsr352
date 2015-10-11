'use strict';

// Declare app level module which depends on views, and components
angular.module('jberetUI', [
  'ngRoute',
  'jberetUI.jobs',
  'jberetUI.jobinstances',
  'jberetUI.jobexecutions',
  'jberetUI.version'
]).
config(['$routeProvider', function($routeProvider) {
  $routeProvider.otherwise({redirectTo: '/jobs'});
}]);
