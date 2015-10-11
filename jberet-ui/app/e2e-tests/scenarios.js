'use strict';

/* https://github.com/angular/protractor/blob/master/docs/toc.md */

describe('my app', function() {


  it('should automatically redirect to /jobs when location hash/fragment is empty', function() {
    browser.get('index.html');
    expect(browser.getLocationAbsUrl()).toMatch("/jobs");
  });


  describe('jobs', function() {

    beforeEach(function() {
      browser.get('index.html#/jobs');
    });


    it('should render jobs when user navigates to /jobs', function() {
      expect(element.all(by.css('[ng-view] p')).first().getText()).
        toMatch(/partial for view 1/);
    });

  });


  describe('jobinstances', function() {

    beforeEach(function() {
      browser.get('index.html#/jobinstances');
    });


    it('should render jobinstances when user navigates to /jobinstances', function() {
      expect(element.all(by.css('[ng-view] p')).first().getText()).
        toMatch(/partial for view 2/);
    });

  });
});
