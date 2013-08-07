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

package org.jberet.job.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.jberet.job.model.JobParser;
import org.jberet.job.model.Split;
import org.jberet.job.model.BatchArtifacts;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.Decision;
import org.jberet.job.model.ExceptionClassFilter;
import org.jberet.job.model.Flow;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Partition;
import org.jberet.job.model.PartitionPlan;
import org.jberet.job.model.Properties;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Step;
import org.jberet.job.model.Transition;
import org.junit.Assert;
import org.junit.Test;

public final class JobParserTest {
    private static final String id1 = "id1";
    private static final String id2 = "id2";
    private static final String class1 = "java.util.Date";
    private static final String class2 = "java.lang.Byte";
    private static final String SAMPLE_JOB_XML = "META-INF/batch-jobs/sample-job.xml";

    @Test
    public void testParseBatchArtifacts() throws Exception {
        final String batchXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +

                        "<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" +
                        "<!-- comments 1 -->" +
                        "<ref id=\"" + id1 + "\" class=\"" + class1 + "\"/>" +
                        "<ref id=\"" + id2 + "\" class=\"" + class2 + "\"/>" +
                        "<!-- comments 2 -->" +
                        "</batch-artifacts>";

        ByteArrayInputStream is = null;
        try {
            is = new ByteArrayInputStream(batchXml.getBytes());
            BatchArtifacts batchArtifacts = JobParser.parseBatchArtifacts(is);
            Assert.assertEquals(class1, batchArtifacts.getClassNameForRef(id1));
            Assert.assertEquals(class2, batchArtifacts.getClassNameForRef(id2));
            Assert.assertNull(batchArtifacts.getClassNameForRef(null));
            Assert.assertNull(batchArtifacts.getClassNameForRef("no such id"));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void testParseJob() throws Exception {
        Job job = null;
        InputStream is = getClass().getClassLoader().getResourceAsStream(SAMPLE_JOB_XML);
        try {
            job = JobParser.parseJob(is);
        } finally {
            is.close();
        }
        Assert.assertEquals("job1", job.getId());
        Assert.assertEquals(true, job.getRestartable());
        checkProperties(job.getProperties());
        checkListeners(job.getListeners());
        for (JobElement element : job.getJobElements()) {
            if (element instanceof Decision) {
                Decision decision = (Decision) element;
                checkDecision(decision, null);
            } else if (element instanceof Step) {
                Step step = (Step) element;
                checkStep(step, null);
            } else if (element instanceof Flow) {
                Flow flow = (Flow) element;
                checkFlow(flow, null);
            } else if (element instanceof Split) {
                Split split = (Split) element;
                checkSplit(split, null);
            }
        }
    }

    private void checkProperties(final Properties properties) throws Exception {
        Assert.assertEquals("partition1", properties.getPartition());
        Assert.assertEquals(2, properties.toJavaUtilProperties().size());
        Assert.assertEquals("value1", properties.get("name1"));
        Assert.assertEquals("value2", properties.get("name2"));
    }

    private void checkListeners(final List<RefArtifact> listeners) throws Exception {
        Assert.assertEquals(2, listeners.size());
        Assert.assertEquals("ref1", listeners.get(0).getRef());
        checkProperties(listeners.get(0).getProperties());
    }

    private void checkDecision(final Decision decision, String parentId) throws Exception {
        String id = "decision1";
        if (parentId != null && parentId.length() > 0) {
            id = parentId + "." + id;
        }
        Assert.assertEquals(id, decision.getId());
        Assert.assertEquals("ref1", decision.getRef());
        checkProperties(decision.getProperties());
        checkTransitionElements(decision.getTransitionElements());
    }

    private void checkFlow(final Flow flow, final String parentId) throws Exception {
        String flowId = "flow1";
        if (parentId != null && parentId.length() > 0) {
            flowId = parentId + "." + flowId;
        }
        Assert.assertEquals(flowId, flow.getId());
        Assert.assertEquals("next1", flow.getAttributeNext());
        checkTransitionElements(flow.getTransitionElements());

        for (JobElement e : flow.getJobElements()) {
            if (e instanceof Decision) {
                checkDecision((Decision) e, flowId);
            } else if (e instanceof Step) {
                checkStep((Step) e, flowId);
            } else if (e instanceof Flow) {
                checkFlow((Flow) e, flowId);
            } else if (e instanceof Split) {
                Split split = (Split) e;
                checkSplit(split, flowId);
            }
        }

    }

    private void checkSplit(final Split split, final String parentId) throws Exception {
        String id = "split1";
        if (parentId != null && parentId.length() > 0) {
            id = parentId + "." + id;
        }
        Assert.assertEquals(id, split.getId());
        Assert.assertEquals("next1", split.getAttributeNext());
        for (Flow f : split.getFlows()) {
            checkFlow(f, id);
        }
    }

    private void checkStep(final Step step, final String parentId) throws Exception {
        String stepId = "step1";
        if (parentId != null && parentId.length() > 0) {
            stepId = parentId + "." + stepId;
        }
        Assert.assertEquals(stepId, step.getId());
        Assert.assertEquals(5, step.getStartLimit());
        Assert.assertEquals(true, step.getAllowStartIfComplete());
        Assert.assertEquals("next1", step.getAttributeNext());
        checkProperties(step.getProperties());
        checkListeners(step.getListeners());
        checkPartition(step.getPartition());
        checkTransitionElements(step.getTransitionElements());

        boolean foundBatchletOrChunk = false;
        if (step.getChunk() != null) {
            foundBatchletOrChunk = true;
            checkChunk(step.getChunk());
        } else if (step.getBatchlet() != null) {
            foundBatchletOrChunk = true;
            String batchletId = "batchlet1";
            if (parentId != null && parentId.length() > 0) {
                batchletId = stepId + "." + batchletId;
            }
            checkRefArtifact(step.getBatchlet(), batchletId);
        }
        Assert.assertEquals(true, foundBatchletOrChunk);
    }

    private void checkChunk(final Chunk chunk) throws Exception {
        Assert.assertEquals("custom", chunk.getCheckpointPolicy());
        Assert.assertEquals(5, chunk.getItemCount());
        Assert.assertEquals(5, chunk.getTimeLimit());
        Assert.assertEquals(5, chunk.getSkipLimit());
        Assert.assertEquals(5, chunk.getSkipLimit());
        checkRefArtifact(chunk.getReader(), "reader1");
        checkRefArtifact(chunk.getProcessor(), "processor1");
        checkRefArtifact(chunk.getWriter(), "writer1");
        checkRefArtifact(chunk.getCheckpointAlgorithm(), "checkpoint-algorithm-1");
        checkExceptionClassFilter(chunk.getSkippableExceptionClasses());
        checkExceptionClassFilter(chunk.getRetryableExceptionClasses());
        checkExceptionClassFilter(chunk.getNoRollbackExceptionClasses());
    }

    private void checkRefArtifact(final RefArtifact a, final String ref) throws Exception {
        Assert.assertEquals(ref, a.getRef());
        checkProperties(a.getProperties());
    }

    private void checkExceptionClassFilter(final ExceptionClassFilter filter) throws Exception {
        final List<String> includes = filter.getInclude();
        final List<String> excludes = filter.getExclude();
        Assert.assertEquals(2, includes.size());
        Assert.assertEquals(true, includes.contains("include1"));
        Assert.assertEquals(true, includes.contains("include2"));
        Assert.assertEquals(2, excludes.size());
        Assert.assertEquals(true, excludes.contains("exclude1"));
        Assert.assertEquals(true, excludes.contains("exclude2"));
    }

    private void checkPartition(final Partition partition) throws Exception {
        checkRefArtifact(partition.getCollector(), "collector1");
        checkRefArtifact(partition.getAnalyzer(), "analyzer1");
        checkRefArtifact(partition.getReducer(), "reducer1");
        boolean foundMapperOrPlan = false;
        if (partition.getMapper() != null) {
            foundMapperOrPlan = true;
            checkRefArtifact(partition.getMapper(), "mapper1");
        } else if (partition.getPlan() != null) {
            foundMapperOrPlan = true;
            checkPlan(partition.getPlan());
        }
    }

    private void checkPlan(final PartitionPlan plan) throws Exception {
        Assert.assertEquals(5, plan.getPartitions());
        Assert.assertEquals(5, plan.getThreads());
        checkProperties(plan.getProperties());
    }

    private void checkTransitionElements(final List<Transition> transitions) throws Exception {
        Assert.assertEquals(4, transitions.size());
        boolean foundNext = false;
        boolean foundStop = false;
        boolean foundFail = false;
        boolean foundEnd = false;
        for (Transition e : transitions) {
            if (e instanceof Transition.Next) {
                Transition.Next next = (Transition.Next) e;
                Assert.assertEquals("on1", next.getOn());
                Assert.assertEquals("to1", next.getTo());
                foundNext = true;
            } else if (e instanceof Transition.Fail) {
                Transition.Fail fail = (Transition.Fail) e;
                Assert.assertEquals("on1", fail.getOn());
                Assert.assertEquals("exit-status1", fail.getExitStatus());
                foundFail = true;
            } else if (e instanceof Transition.End) {
                Transition.End end = (Transition.End) e;
                Assert.assertEquals("on1", end.getOn());
                Assert.assertEquals("exit-status1", end.getExitStatus());
                foundEnd = true;
            } else if (e instanceof Transition.Stop) {
                Transition.Stop stop = (Transition.Stop) e;
                Assert.assertEquals("on1", stop.getOn());
                Assert.assertEquals("exit-status1", stop.getExitStatus());
                Assert.assertEquals("restart1", stop.getRestart());
                foundStop = true;
            }
        }
        Assert.assertEquals(true, foundNext);
        Assert.assertEquals(true, foundStop);
        Assert.assertEquals(true, foundFail);
        Assert.assertEquals(true, foundEnd);
    }
}
