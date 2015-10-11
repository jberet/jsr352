'use strict';

angular.module('jberetUI.version', [
  'jberetUI.version.interpolate-filter',
  'jberetUI.version.version-directive'
])

.value('version', '0.1');
