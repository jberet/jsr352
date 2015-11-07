'use strict';

var angular = require('angular');
require('./utils');

angular.module('jberetUI.common', [])
    .service('batchRestService', ['$http', require('./batchRestService')])
    .service('modalService', ['$uibModal', require('./modalService')]);
