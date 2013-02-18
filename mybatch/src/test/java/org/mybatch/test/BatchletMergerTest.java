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

package org.mybatch.test;

import java.io.Serializable;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mybatch.job.Batchlet;
import org.mybatch.job.Job;
import org.mybatch.job.Step;
import org.mybatch.metadata.JobMerger;
import org.mybatch.metadata.JobXmlLoader;

public class BatchletMergerTest {
    @Test
    public void propertiesFromParentJob() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("batchlet-properties-child.xml", Job.class);
        Batchlet child = getBatchlet(childJob, "batchlet-properties-child");
        Assert.assertNull(child.getProperties());

        JobMerger merger = new JobMerger(childJob);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
        Assert.assertEquals("batchlet-properties-child", child.getRef());
    }

    @Test
    public void mergeFalse() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("batchlet-merge-false-child.xml", Job.class);
        Batchlet child = getBatchlet(childJob, "batchlet-merge-false-child");
        Assert.assertEquals(0, child.getProperties().getProperty().size());

        JobMerger merger = new JobMerger(childJob);
        merger.merge();

        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals("batchlet-merge-false-child", child.getRef());
    }

    @Test
    public void mergeTrue() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("batchlet-merge-true-child.xml", Job.class);
        Batchlet child = getBatchlet(childJob, "batchlet-merge-true-child");
        Assert.assertEquals(1, child.getProperties().getProperty().size());

        JobMerger merger = new JobMerger(childJob);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
        Assert.assertEquals("batchlet-merge-true-child", child.getRef());
    }

    @Test
    public void parentHasBatchlet() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("batchlet-merge-true-child.xml", Job.class);
        JobMerger merger = new JobMerger(childJob);
        merger.merge();
        Batchlet child = getBatchlet(childJob, "parent-has-batchlet-child");

        Assert.assertEquals(1, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent-has-batchlet-parent"}, true);
        Assert.assertEquals("parent-has-batchlet-parent", child.getRef());
    }

    protected static Batchlet getBatchlet(Job job, String stepId) {
        List<Serializable> steps = job.getDecisionOrFlowOrSplit();
        for (Serializable s : steps) {
            if (s instanceof Step) {
                Step step = (Step) s;
                if (stepId.equals(step.getId())) {
                    return step.getBatchlet();
                }
            }
        }
        return null;
    }
}
