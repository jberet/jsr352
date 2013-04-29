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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
    public void concurrentAccess() throws Exception {
        Long jobInstanceId = 1L;
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        Map<String, String> expectedProps = new HashMap<String, String>();
        Task[] tasks = new Task[count];
        for (int i = 0; i < count; i++) {
            expectedProps.put(String.valueOf(i), String.valueOf(i));
            tasks[i] = new Task(jobInstanceId, String.valueOf(i), latch);
        }
        for (Task t : tasks) {
            es.submit(t);
        }
        latch.await();

//TODO
    }

    private static class Task implements Runnable {
        private final String p;
        private final Long jobInstanceId;
        private CountDownLatch latch;

        private Task(Long jobInstanceId, String p, CountDownLatch latch) {
            this.p = p;
            this.jobInstanceId = jobInstanceId;
            this.latch = latch;
        }

        @Override
        public void run() {
//            the following sleep can be used to see the effect of latch.await.  When latch.await
//            and the following block are commented out, the test will fail, because not all of the
//            properties have been saved.
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                //ignore
//            }
            //TODO add some other operations
            latch.countDown();
        }
    }
}
