/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.batch.operations.JobStartException;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class JobMergerTest {
    @Test
    public void stepInheritanceCycle() throws Exception {
        jobStartException("step-inheritance-cycle.xml", "Expecting JobStartException from cyclic inheritance");
    }

    @Test
    public void stepInheritanceSelf() throws Exception {
        jobStartException("step-inheritance-self.xml", "Expecting JobStartException from self inheritance");
    }

    @Test
    public void jobInheritanceSelf() throws Exception {
        jobStartException("job-inheritance-self.xml", "Expecting JobStartException from cyclic inheritance");
    }

    @Test
    public void jobInheritanceCycle() throws Exception {
        jobStartException("job-inheritance-cycle-child.xml", "Expecting JobStartException from cyclic inheritance");
    }

    @Test
    public void jobWithNonexistentParent() throws Exception {
        jobStartException("job-with-nonexistent-parent.xml", "Expecting JobStartException for nonexistent parent");
    }

    @Test
    public void stepWithNonexistentParent() throws Exception {
        jobStartException("step-with-nonexistent-parent.xml", "Expecting JobStartException for nonexistent parent");
    }

    /**
     * Shared by tests expecting JobStartException.
     *
     * @param jobName        the job name to load
     * @param failureMessage the message to include when failing the test
     */
    private void jobStartException(final String jobName, final String failureMessage) {
        try {
            loadJob(jobName);
            Assert.fail(failureMessage);
        } catch (final JobStartException e) {
            e.printStackTrace();
            System.out.printf("Got expected %s%n", e);
        } catch (final Exception e) {
            Assert.fail(failureMessage + ", but got " + e);
        }
    }

    @Test
    public void propertiesListenersFromParentJob() throws Exception {
        //parent job-properties-listeners-parent.xml
        final Job child = loadJob("job-properties-listeners-child.xml");
        Assert.assertEquals("true", child.getRestartable());
        Assert.assertEquals(2, child.getProperties().getNameValues().size());
        Assert.assertEquals(2, child.getListeners().getListeners().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
    }

    @Test
    public void mergeFalse() throws Exception {
        //parent job-merge-false-parent.xml
        final Job child = loadJob("job-merge-false-child.xml");
        Assert.assertEquals("false", child.getRestartable());
        Assert.assertEquals(0, child.getProperties().getNameValues().size());
        Assert.assertEquals(0, child.getListeners().getListeners().size());
    }

    @Test
    public void mergeTrue() throws Exception {
        //parent job-merge-true-parent.xml
        final Job child = loadJob("job-merge-true-child.xml");
        Assert.assertEquals(2, child.getProperties().getNameValues().size());
        Assert.assertEquals(2, child.getListeners().getListeners().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
    }

    /**
     * Verifies that a job xml can reference custom entities for reusing common segments of job definition.
     * This test does not use JSL inheritance.
     *
     * @throws Exception
     * @see <a href="https://issues.jboss.org/browse/JBERET-139">JBERET-139 Implement XMLResolver for Job XML parsing</a>
     */
    @Test
    public void testEntityXMLResolver() throws Exception {
        final String jobName = "job-with-xml-entities";
        Job job = loadJob(jobName);

        Assert.assertEquals(jobName, job.getId());
        Assert.assertEquals(true, job.getRestartableBoolean());
        Assert.assertEquals(null, job.getRestartable());
        Assert.assertEquals(null, job.getParent());
        Assert.assertEquals(null, job.getJslName());

        final List<JobElement> jobElements = job.getJobElements();
        Assert.assertEquals(1, jobElements.size());
        final Step step = (Step) jobElements.get(0);
        Assert.assertEquals(jobName + ".step1", step.getId());
        Assert.assertEquals(null, step.getAttributeNext());
        Assert.assertEquals(null, step.getChunk());

        final RefArtifact batchlet = step.getBatchlet();
        Assert.assertEquals("batchlet1", batchlet.getRef());
        Assert.assertEquals(null, batchlet.getScript());
        Assert.assertEquals(null, batchlet.getProperties());

        //check resolved XML entities
        final Properties properties = job.getProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("common.property.value", properties.get("common.property.key"));
        final List<RefArtifact> listeners = job.getListeners().getListeners();
        Assert.assertEquals(2, listeners.size());
        Assert.assertEquals("EL1", listeners.get(0).getRef());
        Assert.assertEquals("EL2", listeners.get(1).getRef());
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
    public static void propertiesContain(final Properties props, final String[] keys, final boolean... checkValues) throws IllegalStateException {
        final boolean checkVal = checkValues.length == 0 ? false : checkValues[0];
        final java.util.Properties javaUtilProps = Properties.toJavaUtilProperties(props);
        for (final String k : keys) {
            final String v = javaUtilProps.getProperty(k);
            if (v == null) {
                throw new IllegalStateException(String.format("Expecting key %s in properties %s, but found none.", k, javaUtilProps));
            }
            if (checkVal && !v.equals(k)) {
                throw new IllegalStateException(String.format("Expecting property %s : %s, but found %s : %s", k, k, k, v));
            }
        }
    }

    public static List<String> getListenerRefs(final Listeners listeners) {
        final List<String> results = new ArrayList<String>();
        for (final RefArtifact a : listeners.getListeners()) {
            results.add(a.getRef());
        }
        return results;
    }

    public static void listenersContain(final Listeners listeners, final String[] keys) throws IllegalStateException {
        final List<String> refs = getListenerRefs(listeners);
        for (final String k : keys) {
            if (!refs.contains(k)) {
                throw new IllegalStateException(String.format("Expecting ref %s in listeners %s, but found none.", k, refs));
            }
        }
    }

    static Job loadJob(final String jobName) {
        return ArchiveXmlLoader.loadJobXml(jobName, JobMergerTest.class.getClassLoader(), new ArrayList<Job>(), new MetaInfBatchJobsJobXmlResolver());
    }
}
