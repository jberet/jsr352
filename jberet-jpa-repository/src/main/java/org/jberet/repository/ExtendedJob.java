package org.jberet.repository;

import org.jberet.job.model.Job;

public class ExtendedJob {

    private ApplicationAndJobName applicationAndJobName;
    private Job job;

    public ApplicationAndJobName getApplicationAndJobName() {
        return applicationAndJobName;
    }

    public void setApplicationAndJobName(ApplicationAndJobName applicationAndJobName) {
        this.applicationAndJobName = applicationAndJobName;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
    
}
