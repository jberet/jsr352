'use strict';

describe('jberetUI.version module', function() {
  beforeEach(module('jberetUI.version'));

  describe('version service', function() {
    it('should return current version', inject(function(version) {
      expect(version).toEqual('0.1');
    }));
  });
});
