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

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Job;
import org.jberet.repository.JobRepository;
import org.jberet.repository.JobRepositoryFactory;
import org.junit.Assert;
import org.junit.Test;

public class JobRepositoryTest {
    final private static JobRepository repo = JobRepositoryFactory.getJobRepository(null);

    @Test
    public void addRemoveJob() throws Exception {
        final Job job = ArchiveXmlLoader.loadJobXml("exception-class-filter.xml", Job.class, this.getClass().getClassLoader());
        repo.removeJob(job.getId());
        final Collection<Job> jobs = repo.getJobs();
        final int existingJobsCount = jobs.size();

        repo.addJob(job);
        Assert.assertEquals(existingJobsCount + 1, repo.getJobs().size());

        repo.removeJob(job.getId());
        Assert.assertEquals(existingJobsCount, repo.getJobs().size());

        repo.removeJob(job.getId());
        Assert.assertEquals(existingJobsCount, repo.getJobs().size());
    }

}
