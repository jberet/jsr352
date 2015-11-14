'use strict';

var angular = require('angular');
require('angular-ui-router');
require('angular-touch');
require('angular-animate');
require('ui-grid');
require('ui-bootstrap');
require('ui-bootstrap-tpls');
require('csv');
//
//disable pdfmake to save js size. pdfmake requires vfs_fonts.js, which (minified) is over 500k
//require('pdfmake');
//require('vfs_fonts');

require('./common/index');
require('./jobs/jobs');
require('./jobinstances/jobinstances');
require('./jobexecutions/jobexecutions');
require('./details/details');
require('./stepexecution/stepexecution');

// Declare app level module which depends on views, and components
angular.module('jberetUI',
    ['ui.router', 'jberetUI.jobs', 'jberetUI.jobinstances', 'jberetUI.jobexecutions', 'jberetUI.details',
        'jberetUI.common', 'jberetUI.stepexecution'])
    .config(['$urlRouterProvider', function ($urlRouterProvider) {
        $urlRouterProvider.otherwise('/jobs');
    }])
    .config(['$logProvider', function ($logProvider) {
        //to be substituted by gulp with configured debug value
        $logProvider.debugEnabled(/* @echo __DEBUG__ */);
    }]);