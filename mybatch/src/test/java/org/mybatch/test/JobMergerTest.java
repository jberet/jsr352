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

import java.util.ArrayList;
import java.util.List;
import javax.batch.operations.JobStartException;

import org.junit.Assert;
import org.junit.Test;
import org.mybatch.job.Job;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Properties;
import org.mybatch.metadata.ArchiveXmlLoader;
import org.mybatch.metadata.JobMerger;
import org.mybatch.util.BatchUtil;

public class JobMergerTest {
    @Test
    public void stepInheritanceCycle() throws Exception {
        Job child = ArchiveXmlLoader.loadJobXml("step-inheritance-cycle.xml", Job.class);
        jobStartException(child, "Expecting exceptioin from cyclic inheritance, but got no exception.");
    }

    @Test
    public void stepInheritanceSelf() throws Exception {
        Job child = ArchiveXmlLoader.loadJobXml("step-inheritance-self.xml", Job.class);
        jobStartException(child, "Expecting exceptioin from self inheritance, but got no exception.");
    }

    @Test
    public void jobInheritanceSelf() throws Exception {
        Job child = ArchiveXmlLoader.loadJobXml("job-inheritance-self.xml", Job.class);
        jobStartException(child, "Expecting exception from cyclic inheritance, but got no exceptioin");
    }

    @Test
    public void jobInheritanceCycle() throws Exception {
        Job child = ArchiveXmlLoader.loadJobXml("job-inheritance-cycle-child.xml", Job.class);
        jobStartException(child, "Expecting exception from cyclic inheritance, but got no exceptioin");
    }

    /**
     * Shared by tests expecting JobStartException.
     *
     * @param child          the job to resolve its inheritance
     * @param failureMessage the message to include when failing the test
     */
    private void jobStartException(Job child, String failureMessage) {
        try {
            JobMerger merger = new JobMerger(child);
            merger.merge();
            Assert.fail(failureMessage);
        } catch (JobStartException e) {
            System.out.printf("Got expected %s%n", e);
        }
    }

    @Test
    public void propertiesListenersFromParentJob() throws Exception {
        Job parent = ArchiveXmlLoader.loadJobXml("job-properties-listeners-parent.xml", Job.class);
        Job child = ArchiveXmlLoader.loadJobXml("job-properties-listeners-child.xml", Job.class);

        Assert.assertNull(child.getRestartable());
        Assert.assertEquals(true, child.getProperties() == null || child.getProperties().getProperty().size() == 0);
        Assert.assertEquals(true, child.getListeners() == null || child.getListeners().getListener().size() == 0);

//        JobMerger merger = new JobMerger(parent, child);
        JobMerger merger = new JobMerger(child);
        merger.merge();

        Assert.assertEquals("true", child.getRestartable());
        Assert.assertEquals(2, child.getProperties().getProperty().size());
        Assert.assertEquals(2, child.getListeners().getListener().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
    }

    @Test
    public void mergeFalse() throws Exception {
        Job parent = ArchiveXmlLoader.loadJobXml("job-merge-false-parent.xml", Job.class);
        Job child = ArchiveXmlLoader.loadJobXml("job-merge-false-child.xml", Job.class);

        Assert.assertEquals("false", child.getRestartable());
        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals(0, child.getListeners().getListener().size());

//        JobMerger merger = new JobMerger(parent, child);
        JobMerger merger = new JobMerger(child);
        merger.merge();

        Assert.assertEquals("false", child.getRestartable());
        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals(0, child.getListeners().getListener().size());
    }

    @Test
    public void mergeTrue() throws Exception {
        Job parent = ArchiveXmlLoader.loadJobXml("job-merge-true-parent.xml", Job.class);
        Job child = ArchiveXmlLoader.loadJobXml("job-merge-true-child.xml", Job.class);

        Assert.assertEquals(1, child.getProperties().getProperty().size());
        Assert.assertEquals(1, child.getListeners().getListener().size());

//        JobMerger merger = new JobMerger(parent, child);
        JobMerger merger = new JobMerger(parent, child);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        Assert.assertEquals(2, child.getListeners().getListener().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
    }

    /**
     * verifies that the Properties (generated jaxb type, not java.util.Properties) props contains every key in keys.
     * As a testing convention, the value is the same as the key.  For instance, the Properties can be:
     * "foo": "foo", "bar": "bar"
     *
     * @param props       Properties from job xml
     * @param keys        a String array of keys
     * @param checkValues whether to check property value
     * @throws IllegalStateException if any key is not found
     */
    public static void propertiesContain(Properties props, String[] keys, boolean... checkValues) throws IllegalStateException {
        boolean checkVal = checkValues.length == 0 ? false : checkValues[0];
        java.util.Properties javaUtilProps = BatchUtil.toJavaUtilProperties(props);
        for (String k : keys) {
            String v = javaUtilProps.getProperty(k);
            if (v == null) {
                throw new IllegalStateException(String.format("Expecting key %s in properties %s, but found none.", k, javaUtilProps));
            }
            if (checkVal && !v.equals(k)) {
                throw new IllegalStateException(String.format("Expecting property %s : %s, but found %s : %s", k, k, k, v));
            }
        }
    }

    public static List<String> getListenerRefs(Listeners listeners) {
        List<String> results = new ArrayList<String>();
        List<Listener> listenerList = listeners.getListener();
        for (Listener l : listenerList) {
            results.add(l.getRef());
        }
        return results;
    }

    public static void listenersContain(Listeners listeners, String[] keys) throws IllegalStateException {
        List<String> refs = getListenerRefs(listeners);
        for (String k : keys) {
            if (!refs.contains(k)) {
                throw new IllegalStateException(String.format("Expecting ref %s in listeners %s, but found none.", k, refs));
            }
        }
    }
}
