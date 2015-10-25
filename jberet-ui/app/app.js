'use strict';

// Declare app level module which depends on views, and components
angular.module('jberetUI',
    ['ui.router', 'jberetUI.jobs', 'jberetUI.jobinstances', 'jberetUI.jobexecutions', 'jberetUI.details',
        'jberetUI.stepexecution', 'jberetUI.version']).

    config(['$urlRouterProvider', function ($urlRouterProvider) {
        $urlRouterProvider.otherwise('/jobs');
    }]);

var jberetui = {
    parseJobParameters: function (keyValues) {
        if (keyValues == null) {
            return null;
        }
        keyValues = keyValues.trim();
        if (keyValues.length == 0) {
            return null;
        }
        var result = {};
        var lines = keyValues.split(/\r\n|\r|\n/g);
        var x;
        for (x in lines) {
            var line = lines[x].trim();
            if (line.length == 0) {
                continue;
            }
            var pair = line.split('=');
            var key = pair[0].trim();
            result[key] = pair.length > 1 ? pair[1].trim() : '';
        }
        return result;
    },

    getIdFromUrl: function (url, tokenBeforeId) {
        var result = null;
        var tokenStartPos = url.lastIndexOf(tokenBeforeId);
        if (tokenStartPos >= 0) {
            var startOfId = tokenStartPos + tokenBeforeId.length;
            var stopPos = url.indexOf('/', startOfId + 1);
            if (stopPos < 0) {
                stopPos = url.length;
            }
            result = url.substring(startOfId, stopPos);
        }
        return result;
    },

    getColor: function (data) {
        return data == 'COMPLETED' ? 'text-success' :
            data == 'FAILED' || data == 'ABANDONED' ? 'text-danger' :
                data == 'STOPPED' || data == 'STOPPING' ? 'text-warning' :
                    'text-primary';
    },

    formatAsKeyValuePairs: function (obj) {
        var result = '';
        if (!obj) {
            return result;
        }
        for (var p in obj) {
            if(obj.hasOwnProperty(p)) {
                result = result + p + ' = ' + obj[p] + ', ';
            }
        }
        if (result.length > 1) {
            result = result.substring(0, result.length - 2);
        }
        return result;
    }
};
