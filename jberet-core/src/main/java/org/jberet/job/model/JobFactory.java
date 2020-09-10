package org.jberet.job.model;

import java.util.List;

public class JobFactory {
    private static final JobMapper jobMapper = new JobMapperImpl();

    public static Job cloneJob(Job job) {
        return jobMapper.job(job);
    }

    public static Step cloneStep(Step step) {
        return jobMapper.step(step);
    }

    public static List<JobElement> cloneJobElements(List<JobElement> jobElements) {
        return jobMapper.jobElements(jobElements);
    }
}
