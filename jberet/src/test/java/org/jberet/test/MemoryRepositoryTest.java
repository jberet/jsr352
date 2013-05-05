/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jberet.job.Job;
import org.jberet.metadata.ArchiveXmlLoader;
import org.jberet.repository.impl.MemoryRepository;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class MemoryRepositoryTest {
    final private static MemoryRepository repo = MemoryRepository.INSTANCE;
    private Job job;
    private static ExecutorService es = Executors.newCachedThreadPool();

    @AfterClass
    public static void afterClass() throws Exception {
        es.shutdownNow();
    }

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

    @Test
    public void nextUniqueId() throws Exception {
        int count = 20;
        final List<Long> results = Collections.synchronizedList(new ArrayList<Long>());
        for (int i = 0; i < count; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    results.add(repo.nextUniqueId());
                }
            };
            es.submit(runnable);
        }
        synchronized (results) {
            for (Long l : results) {
                Assert.assertEquals(1, Collections.frequency(results, l));
            }
        }
    }
}
