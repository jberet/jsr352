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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.batch.operations.BatchRuntimeException;
import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;

import static org.jberet.util.BatchUtil.NL;

public final class JobParserTest {
    private static final String id1 = "id1";
    private static final String id2 = "id2";
    private static final String id3 = "id3";
    private static final String class1 = "java.util.Date";
    private static final String class2 = "java.lang.Byte";
    private static final String class3 = "java.lang.Integer";
    private static final String SAMPLE_JOB_XML = "META-INF/batch-jobs/sample-job.xml";

    @Test
    public void testParseBatchArtifacts() throws Exception {
        final String batchXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                        "<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + NL +
                        "<!-- comments 1 -->" + NL +
                        "<ref id=\"" + id1 + "\" class=\"" + class1 + "\"/>" + NL +
                        "<ref id=\"" + id2 + "\" class=\"" + class2 + "\"/>" + NL +
                        "<!-- comments 2 -->" + NL +
                        "</batch-artifacts>" + NL;

        ByteArrayInputStream is = null;
        try {
            is = new ByteArrayInputStream(batchXml.getBytes());
            final BatchArtifacts batchArtifacts = JobParser.parseBatchArtifacts(is);
            Assert.assertEquals(class1, batchArtifacts.getClassNameForRef(id1));
            Assert.assertEquals(class2, batchArtifacts.getClassNameForRef(id2));
            Assert.assertNull(batchArtifacts.getClassNameForRef(null));
            Assert.assertNull(batchArtifacts.getClassNameForRef(""));
            Assert.assertNull(batchArtifacts.getClassNameForRef("no such id"));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void testBatchXmlMissingEndBatchArtifacts() throws Exception {
        final String batchXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                        "<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + NL +
                        "<!-- comments 1 -->" + NL +
                        "<ref id=\"" + id1 + "\" class=\"" + class1 + "\"/>" + NL +
                        "<ref id=\"" + id2 + "\" class=\"" + class2 + "\"/>" + NL;
//                        "</batch-artifacts>" + NL;     missing root element closing

        checkInvalidBatchXml(batchXml);
    }

    @Test
    public void testBatchXmlMissingEndRef() throws Exception {
        final String batchXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                        "<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + NL +
                        "<!-- comments 1 -->" + NL +
                        "<ref id=\"" + id1 + "\" class=\"" + class1 + "\">" + NL +
                        "</ref>" + NL +
                        "<ref id=\"" + id2 + "\" class=\"" + class2 + "\">" + NL +    //missing ref element closing
                        "<ref id=\"" + id3 + "\" class=\"" + class3 + "\"/>" + NL +
                        "</batch-artifacts>" + NL;

        checkInvalidBatchXml(batchXml);
    }


    @Test
    public void testBatchXmlUnknownElement() throws Exception {
        final String batchXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                        "<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + NL +
                        "<ref id=\"" + id1 + "\" class=\"" + class1 + "\"/>" + NL +
                        "<ref1 id=\"" + id2 + "\" class=\"" + class2 + "\"/>" + NL +
                        "</batch-artifacts>" + NL;
        checkInvalidBatchXml(batchXml);
    }

    @Test
    public void testBatchXmlWrongAttribute() throws Exception {
        final String batchXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                        "<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + NL +
                        "<ref id=\"" + id1 + "\" value=\"" + class1 + "\"/>" + NL +
                        "</batch-artifacts>" + NL;
        checkInvalidBatchXml(batchXml);
    }

    @Test
    public void testJobXmlUnknownElement() throws Exception {
        final String batchXml =
        "<job id=\"job1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"1.0\">" + NL +
          "<step id=\"step1\">" + NL +
            "<properties>" + NL +
              "<property name=\"step-prop\" value=\"step-prop\"/>" + NL +
            "</properties>" + NL +
            "<batchlet ref=\"batchlet1\">" + NL +
              "<reader ref=\"R1\"></reader>" + NL +     //unexpected element
            "</batchlet>" + NL +
          "</step>" + NL +
        "</job>" + NL;
        checkInvalidJobXml(batchXml);
    }

    @Test
    public void testJobXmlWrongAttribute() throws Exception {
        final String batchXml =
        "<job id=\"job1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"1.0\">" + NL +
          "<step id=\"step1\">" + NL +
            "<batchlet id=\"batchlet1\">" + NL +        //the attribute should be "ref"
            "</batchlet>" + NL +
          "</step>" + NL +
        "</job>" + NL;
        checkInvalidJobXml(batchXml);
    }

    @Test
    public void testJobXmlBothNextAttributeAndElementInStep() throws Exception {
        final String batchXml =
                "<job id=\"job1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"1.0\">" + NL +
                        "<step id=\"step1\" next=\"step2\">" + NL +
                        "<batchlet ref=\"batchlet1\">" + NL +
                        "</batchlet>" + NL +
                        "<next on=\"*\" to=\"step2\">" + NL +
                        "</step>" + NL +
                        "</job>" + NL;
        checkInvalidJobXml(batchXml);
    }

    @Test
    public void testJobXmlBothNextAttributeAndElementInFlow() throws Exception {
        final String batchXml =
                "<job id=\"job1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"1.0\">" + NL +
                        "<flow id=\"flow1\" next=\"next1\">" + NL +

                        "<step id=\"step1\">" + NL +
                        "<batchlet ref=\"batchlet1\">" + NL +
                        "</batchlet>" + NL +
                        "</step>" + NL +

                        "<next on=\"?\" to=\"next1\">" + NL +
                        "</flow>" + NL +
                        "</job>" + NL;
        checkInvalidJobXml(batchXml);
    }

    @Test
    public void testParseJob() throws Exception {
        Job job = null;
        final InputStream is = getClass().getClassLoader().getResourceAsStream(SAMPLE_JOB_XML);
        try {
            job = JobParser.parseJob(is, getClass().getClassLoader());
        } finally {
            is.close();
        }
        Assert.assertEquals("job1", job.getId());
        Assert.assertEquals(true, job.getRestartableBoolean());
        checkProperties(job.getProperties());
        checkListeners(job.getListeners());
        for (final JobElement element : job.getJobElements()) {
            if (element instanceof Decision) {
                final Decision decision = (Decision) element;
                checkDecision(decision, null);
            } else if (element instanceof Step) {
                final Step step = (Step) element;
                checkStep(step, null);
            } else if (element instanceof Flow) {
                final Flow flow = (Flow) element;
                checkFlow(flow, null);
            } else if (element instanceof Split) {
                final Split split = (Split) element;
                checkSplit(split, null);
            } else {
                Assert.fail("Unexpected job element type: " + element);
            }
        }
    }

    private void checkInvalidBatchXml(final String xmlContent) throws IOException {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(xmlContent.getBytes());
            JobParser.parseBatchArtifacts(is);
            Assert.fail("Exception should already have been thrown.");
        } catch (XMLStreamException e) {
            System.out.printf("Got expected exception %s%n", e);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got expected exception %s%n", e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void checkInvalidJobXml(final String xmlContent) throws IOException {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(xmlContent.getBytes());
            JobParser.parseJob(is, getClass().getClassLoader());
            Assert.fail("Exception should already have been thrown.");
        } catch (XMLStreamException e) {
            System.out.printf("Got expected exception %s%n", e);
        } catch (BatchRuntimeException e) {
            System.out.printf("Got expected exception %s%n", e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void checkProperties(final Properties properties, final String... partitionNumbers) throws Exception {
        final String partitionNumber = partitionNumbers.length == 0 ? "0" : partitionNumbers[0];
        Assert.assertEquals(partitionNumber, properties.getPartition());
        Assert.assertEquals(2, Properties.toJavaUtilProperties(properties).size());
        Assert.assertEquals("value1", properties.get("name1"));
        Assert.assertEquals("value2", properties.get("name2"));
        Assert.assertNull(properties.get(""));
    }

    private void checkListeners(final Listeners listeners) throws Exception {
        final List<RefArtifact> listenerList = listeners.getListeners();
        Assert.assertEquals(2, listenerList.size());
        Assert.assertEquals("ref1", listenerList.get(0).getRef());
        checkProperties(listenerList.get(0).getProperties());
        Assert.assertEquals("ref2", listenerList.get(1).getRef());
        checkProperties(listenerList.get(1).getProperties());
    }

    private void checkDecision(final Decision decision, final String parentId) throws Exception {
        String id = "decision1";
        if (parentId != null && parentId.length() > 0) {
            id = parentId + "." + id;
        }
        Assert.assertEquals(id, decision.getId());
        Assert.assertEquals("ref1", decision.getRef());
        checkProperties(decision.getProperties());
        checkTransitionElements(decision.getTransitionElements(), null);
    }

    private void checkFlow(final Flow flow, final String parentId) throws Exception {
        String flowId = "flow1";
        if (parentId != null && parentId.length() > 0) {
            flowId = parentId + "." + flowId;
        }
        Assert.assertEquals(flowId, flow.getId());
//        Assert.assertEquals("next1", flow.getAttributeNext());    defer this check to checkTransitionElements
        checkTransitionElements(flow.getTransitionElements(), flow.getAttributeNext());

        for (final JobElement e : flow.jobElements) {
            if (e instanceof Decision) {
                checkDecision((Decision) e, flowId);
            } else if (e instanceof Step) {
                checkStep((Step) e, flowId);
            } else if (e instanceof Flow) {
                checkFlow((Flow) e, flowId);
            } else if (e instanceof Split) {
                final Split split = (Split) e;
                checkSplit(split, flowId);
            } else {
                Assert.fail("Unexpected job element type inside flow: " + e);
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
        for (final Flow f : split.getFlows()) {
            checkFlow(f, id);
        }
    }

    private void checkStep(final Step step, final String parentId) throws Exception {
        String stepId = "step1";
        if (parentId != null && parentId.length() > 0) {
            stepId = parentId + "." + stepId;
        }
        Assert.assertEquals(stepId, step.getId());
        Assert.assertEquals(5, step.getStartLimitInt());
        Assert.assertEquals(true, step.getAllowStartIfCompleteBoolean());
//        Assert.assertEquals("next1", step.getAttributeNext());    defer this check to checkTransitionElements() along with next element
        checkProperties(step.getProperties());
        checkListeners(step.getListeners());
        checkPartition(step.getPartition());
        checkTransitionElements(step.getTransitionElements(), step.getAttributeNext());

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
        Assert.assertEquals(5, chunk.getItemCountInt());
        Assert.assertEquals(5, chunk.getTimeLimitInt());
        Assert.assertEquals(5, chunk.getSkipLimitInt());
        Assert.assertEquals(5, chunk.getSkipLimitInt());
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
        final List<String> includes = filter.include;
        final List<String> excludes = filter.exclude;
        Assert.assertEquals(2, includes.size());
        Assert.assertEquals(true, includes.contains("include1"));
        Assert.assertEquals(true, includes.contains("include2"));
        Assert.assertEquals(false, includes.contains(""));
        Assert.assertEquals(2, excludes.size());
        Assert.assertEquals(true, excludes.contains("exclude1"));
        Assert.assertEquals(true, excludes.contains("exclude2"));
        Assert.assertEquals(false, excludes.contains(""));
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
        Assert.assertEquals(true, foundMapperOrPlan);
    }

    private void checkPlan(final PartitionPlan plan) throws Exception {
        Assert.assertEquals(5, plan.getPartitionsInt());
        Assert.assertEquals(5, plan.getThreadsInt());

        //partition attribute values are: "0", "1", etc
        for (int i = 0; i < plan.getPropertiesList().size(); i++) {
            checkProperties(plan.getPropertiesList().get(i), String.valueOf(i));
        }
    }

    private void checkTransitionElements(final List<Transition> transitions, final String nextAttributeValue) throws Exception {
        final int transitionElementCount = nextAttributeValue == null ? 4 : 3;
        Assert.assertEquals(transitionElementCount, transitions.size());
        boolean foundNext = false;
        boolean foundStop = false;
        boolean foundFail = false;
        boolean foundEnd = false;
        for (final Transition e : transitions) {
            if (e instanceof Transition.Next) {
                if (nextAttributeValue != null) {
                    Assert.fail("Cannot have both next attribute and next element. Next attribute is already set to " + nextAttributeValue);
                }
                final Transition.Next next = (Transition.Next) e;
                Assert.assertEquals("on1", next.getOn());
                Assert.assertEquals("to1", next.getTo());
                foundNext = true;
            } else if (e instanceof Transition.Fail) {
                final Transition.Fail fail = (Transition.Fail) e;
                Assert.assertEquals("on1", fail.getOn());
                Assert.assertEquals("exit-status1", fail.getExitStatus());
                foundFail = true;
            } else if (e instanceof Transition.End) {
                final Transition.End end = (Transition.End) e;
                Assert.assertEquals("on1", end.getOn());
                Assert.assertEquals("exit-status1", end.getExitStatus());
                foundEnd = true;
            } else if (e instanceof Transition.Stop) {
                final Transition.Stop stop = (Transition.Stop) e;
                Assert.assertEquals("on1", stop.getOn());
                Assert.assertEquals("exit-status1", stop.getExitStatus());
                Assert.assertEquals("restart1", stop.getRestart());
                foundStop = true;
            } else {
                Assert.fail("Unexpected job transition type: " + e);
            }
        }
        if (nextAttributeValue == null) {
            Assert.assertEquals(true, foundNext);
        } else {
            Assert.assertEquals(false, foundNext);
            Assert.assertEquals("next1", nextAttributeValue);
        }
        Assert.assertEquals(true, foundStop);
        Assert.assertEquals(true, foundFail);
        Assert.assertEquals(true, foundEnd);
    }
}
