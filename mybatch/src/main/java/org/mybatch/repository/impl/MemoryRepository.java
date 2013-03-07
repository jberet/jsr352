/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.mybatch.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.mybatch.job.Job;
import org.mybatch.repository.JobRepository;

public class MemoryRepository implements JobRepository {
    private final AtomicLong atomicLong = new AtomicLong();

    private ConcurrentMap<String, Job> jobs = new ConcurrentHashMap<String, Job>();

    @Override
    public long nextUniqueId() {
        return atomicLong.incrementAndGet();
    }

    @Override
    public Job addJob(Job job) {
        return jobs.putIfAbsent(job.getId(), job);
    }

    @Override
    public Job removeJob(String jobId) {
        //may need to cascade-remove all the associated jobInstances
        return jobs.remove(jobId);
    }

    @Override
    public Job findJob(String jobId) {
        return jobs.get(jobId);
    }

    @Override
    public Collection<Job> getJobs() {
        return Collections.unmodifiableCollection(jobs.values());
    }

}
