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
import org.mybatch.job.Job;
import org.mybatch.job.Step;
import org.mybatch.metadata.StepMerger;
import org.mybatch.metadata.JobXmlLoader;

public class StepMergerTest {
    @Test
    public void propertiesListenersFromParentJob() throws Exception {
        Job parentJob = JobXmlLoader.loadJobXml("step-properties-listeners-parent.xml", Job.class);
        Step parent = getStep(parentJob, "step-properties-listeners-parent");

        Job childJob = JobXmlLoader.loadJobXml("step-properties-listeners-child.xml", Job.class);
        Step child = getStep(childJob, "step-properties-listeners-child");

        Assert.assertNull(child.getProperties());
        Assert.assertNull(child.getListeners());

        StepMerger merger = new StepMerger(parent, child);
//        StepMerger merger = new StepMerger(child, (List<Step>) null);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
        Assert.assertEquals(2, child.getListeners().getListener().size());
    }

    @Test
    public void mergeFalse() throws Exception {
        Job parentJob = JobXmlLoader.loadJobXml("step-merge-false-parent.xml", Job.class);
        Step parent = getStep(parentJob, "step-merge-false-parent");

        Job childJob = JobXmlLoader.loadJobXml("step-merge-false-child.xml", Job.class);
        Step child = getStep(childJob, "step-merge-false-child");

        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals(0, child.getListeners().getListener().size()) ;

        StepMerger merger = new StepMerger(parent, child);
//        StepMerger merger = new StepMerger(child, (List<Step>) null);
        merger.merge();

        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals(0, child.getListeners().getListener().size());
    }

    @Test
    public void mergeTrue() throws Exception {
        Job parentJob = JobXmlLoader.loadJobXml("step-merge-true-parent.xml", Job.class);
        Step parent = getStep(parentJob, "step-merge-true-parent");

        Job childJob = JobXmlLoader.loadJobXml("step-merge-true-child.xml", Job.class);
        Step child = getStep(childJob, "step-merge-true-child");

        Assert.assertEquals(1, child.getProperties().getProperty().size());
        Assert.assertEquals(1, child.getListeners().getListener().size()) ;

        StepMerger merger = new StepMerger(parent, child);
//        StepMerger merger = new StepMerger(child, (List<Step>) null);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
        Assert.assertEquals(2, child.getListeners().getListener().size());
    }

    protected static Step getStep(Job job, String stepId) {
        List<Serializable> steps = job.getDecisionOrFlowOrSplit();
        for (Serializable s : steps) {
            if (s instanceof Step) {
                if (stepId.equals(((Step) s).getId())) {
                    return (Step) s;
                }
            }
        }
        return null;
    }

}
