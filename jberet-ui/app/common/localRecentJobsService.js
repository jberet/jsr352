'use strict';

var localRecentJobsService = function () {
    var recentJobsLimit = 10;
    var recentJobsKey = 'jberet.ui.recentJobs';
    var jobSeparator = '|';

    this.saveToLocalRecentJobs = function(jobName) {
        if (jobName && typeof(Storage) !== "undefined" && recentJobsLimit > 0) {
            var existingEntries = localStorage.getItem(recentJobsKey);
            if(existingEntries) {
                var tokens = existingEntries.split(jobSeparator);
                if(tokens.indexOf(jobName) < 0) {
                    //not found in the existing local storage recent job names, and need to add it
                    //first check size
                    if(tokens.length >= recentJobsLimit) {
                        //already reached the max limit, so remove the last element
                        tokens.pop();
                    }
                    //and add the new job name to the head
                    tokens.unshift(jobName);
                    localStorage.setItem(recentJobsKey, tokens.join(jobSeparator));
                }
            } else {
                localStorage.setItem(recentJobsKey, jobName);
            }
        }
    };

    this.getLocalRecentJobs = function() {
        if (typeof(Storage) !== "undefined" && recentJobsLimit > 0) {
            var existingEntries = localStorage.getItem(recentJobsKey);
            if(existingEntries) {
                return existingEntries.split(jobSeparator);
            }
        }
        return null;
    };
};

module.exports = localRecentJobsService;