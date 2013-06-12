/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.test;

import java.util.Collection;

import org.jberet.job.Job;
import org.jberet.metadata.ArchiveXmlLoader;
import org.jberet.repository.JobRepository;
import org.jberet.repository.JobRepositoryFactory;
import org.junit.Assert;
import org.junit.Test;

public class JobRepositoryTest {
    final private static JobRepository repo = JobRepositoryFactory.getJobRepository();
    private Job job;

    @Test
    public void addRemoveJob() throws Exception {
        job = ArchiveXmlLoader.loadJobXml("batchlet1.xml", Job.class);
        repo.removeJob(job.getId());
        Collection<Job> jobs = repo.getJobs();
        int existingJobsCount = jobs.size();

        boolean isAdded = repo.addJob(job);
        Assert.assertEquals(true, isAdded);  //the first time, no pre-existing job by the same jobId.
        Assert.assertEquals(existingJobsCount + 1, repo.getJobs().size());

        boolean isRemoved = repo.removeJob(job.getId());
        Assert.assertEquals(true, isRemoved);
        Assert.assertEquals(existingJobsCount, repo.getJobs().size());

        isRemoved = repo.removeJob(job.getId());
        Assert.assertEquals(false, isRemoved);
        Assert.assertEquals(existingJobsCount, repo.getJobs().size());
    }

}
