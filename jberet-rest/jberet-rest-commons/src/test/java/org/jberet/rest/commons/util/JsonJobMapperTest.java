/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.rest.commons.util;

import java.util.List;

import org.jberet.job.model.Chunk;
import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Properties;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;
import org.jberet.job.model.Transition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Tests to verify JSON job definition content is property converted into
 * job object.
 *
 * @see JsonJobMapper
 * @since 1.3.0.Final
 */
public final class JsonJobMapperTest {
    @Test(expected = IllegalStateException.class)
    public void missingJobId() throws Exception {
        String json = "{\n" +
                "  \"job\": {\n" +
                "    \"step\": {\n" +
                "      \"id\": \"simple.step1\",\n" +
                "      \"chunk\": {\n" +
                "        \"reader\": { \"ref\": \"arrayItemReader\" },\n" +
                "        \"writer\": { \"ref\": \"mockItemWriter\" }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
    }

    @Test(expected = IllegalStateException.class)
    public void missingStepId() throws Exception {
        String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"simple\",\n" +
                "    \"step\": {\n" +
                "      \"chunk\": {\n" +
                "        \"reader\": { \"-ref\": \"arrayItemReader\" },\n" +
                "        \"writer\": { \"-ref\": \"mockItemWriter\" }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
    }

    @Test(expected = IllegalStateException.class)
    public void missingListenerRef() throws Exception {
        String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"listeners\": {\n" +
                "      \"listener\": { \"xxx\": \"xxx\" }\n" +
                "    },\n" +
                "    \"step\": {\n" +
                "      \"id\": \"step1\",\n" +
                "      \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
    }

    /**
     * Verifies a job with one chunk-type step.
     * This one step node is of object type.
     *
     * @throws Exception
     */
    @Test
    public void job1() throws Exception {
        String json =
                "{\n" +
                        "  \"job\": {\n" +
                        "    \"id\": \"simple\",\n" +
                        "    \"step\": {\n" +
                        "      \"id\": \"simple.step1\",\n" +
                        "      \"chunk\": {\n" +
                        "        \"reader\": {\n" +
                        "          \"ref\": \"arrayItemReader\",\n" +
                        "          \"properties\": {\n" +
                        "            \"property\": [\n" +
                        "              {\n" +
                        "                \"name\": \"resource\",\n" +
                        "                \"value\": \"[0, 1, 2, 3]\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"name\": \"beanType\",\n" +
                        "                \"value\": \"java.lang.Integer\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"name\": \"skipBeanValidation\",\n" +
                        "                \"value\": \"true\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"name\": \"start\",\n" +
                        "                \"value\": \"5\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"name\": \"end\",\n" +
                        "                \"value\": \"10\"\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"writer\": { \"ref\": \"mockItemWriter\" }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
        Job job = JsonJobMapper.toJob(json);
        assertEquals("simple", job.getId());
        assertEquals(true, job.getRestartableBoolean());
        assertEquals(null, job.getListeners());
        assertEquals(null, job.getProperties());

        final Step step1 = (Step) job.getJobElements().get(0);
        assertEquals("simple.step1", step1.getId());
        assertEquals(false, step1.getAllowStartIfCompleteBoolean());
        assertEquals(0, step1.getStartLimitInt());
        assertEquals(null, step1.getAttributeNext());
        assertEquals(null, step1.getBatchlet());
        assertEquals(null, step1.getProperties());
        assertEquals(null, step1.getPartition());
        assertEquals(null, step1.getListeners());
        assertEquals(true, step1.getTransitionElements() == null ||
                step1.getTransitionElements().size() == 0);

        final Chunk chunk = step1.getChunk();
        assertEquals(null, chunk.getCheckpointAlgorithm());
        assertEquals(null, chunk.getProcessor());
        assertEquals(null, chunk.getNoRollbackExceptionClasses());
        assertEquals(null, chunk.getSkippableExceptionClasses());
        assertEquals(null, chunk.getRetryableExceptionClasses());
        assertEquals("item", chunk.getCheckpointPolicy());
        assertEquals(null, chunk.getItemCount());
        assertEquals(null, chunk.getRetryLimit());
        assertEquals(null, chunk.getSkipLimit());
        assertEquals(null, chunk.getTimeLimit());

        final RefArtifact reader = chunk.getReader();
        assertEquals("arrayItemReader", reader.getRef());
        assertEquals(5, reader.getProperties().size());
        assertEquals("5", reader.getProperties().get("start"));

        final RefArtifact writer = chunk.getWriter();
        assertEquals("mockItemWriter", writer.getRef());
        assertEquals(true, writer.getProperties() == null ||
                writer.getProperties().size() == 0);
    }

    /**
     * Verifies a job with 2 steps.
     * The 2 steps are both under the step node whose value is of type array.
     *
     * @throws Exception
     */
    @Test
    public void twoSteps() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"restartable\": \"false\",\n" +
                "    \"step\": [\n" +
                "      {\n" +
                "        \"id\": \"step1\",\n" +
                "        \"next\": \"step2\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"step2\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals("job1", job.getId());
        assertFalse(job.getRestartableBoolean());
        final List<JobElement> jobElements = job.getJobElements();
        assertEquals(2, jobElements.size());
        Step step1 = (Step) jobElements.get(0);
        assertEquals("step1", step1.getId());
        assertEquals("step2", step1.getAttributeNext());
        assertEquals(null, step1.getChunk());
        assertEquals("batchlet1", step1.getBatchlet().getRef());

        Step step2 = (Step) jobElements.get(1);
        assertEquals("step2", step2.getId());
        assertEquals(null, step2.getAttributeNext());
        assertEquals(null, step2.getChunk());
        assertEquals("batchlet2", step2.getBatchlet().getRef());
        assertEquals(0, step2.getBatchlet().getProperties().size());
    }

    /**
     * Verifies a step that contains one or multiple transition elements:
     * end, fail, stop & next and each of them can appear one or multiple times.
     *
     * @throws Exception
     */
    @Test
    public void stepWithTransitionElements() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"step\": [\n" +
                "      {\n" +
                "        \"id\": \"step1\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet1\" },\n" +
                "        \"next\": {\n" +
                "          \"on\": \"next1\",\n" +
                "          \"to\": \"step1\"\n" +
                "        },\n" +
                "        \"fail\": {\n" +
                "          \"on\": \"fail1\",\n" +
                "          \"exit-status\": \"x\"\n" +
                "        },\n" +
                "        \"end\": {\n" +
                "          \"on\": \"end1\",\n" +
                "          \"exit-status\": \"x\"\n" +
                "        },\n" +
                "        \"stop\": {\n" +
                "          \"on\": \"stop1\",\n" +
                "          \"exit-status\": \"x\",\n" +
                "          \"restart\": \"step1\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"step2\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals(2, job.getJobElements().size());
        final Step step1 = (Step) job.getJobElements().get(0);
        assertEquals("step1", step1.getId());
        verifyTransitionElements(step1.getTransitionElements());
    }

    /**
     * Verifies a flow that contains 2 steps and transition elements:
     * end, fail, stop & next, and each of the appear once.
     * 
     * @throws Exception
     */
    @Test
    public void flowWithTransitionElements() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"flow\": {\n" +
                "      \"id\": \"flow1\",\n" +
                "      \"step\": [\n" +
                "        {\n" +
                "          \"id\": \"step1\",\n" +
                "          \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"step2\",\n" +
                "          \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"next\": {\n" +
                "        \"on\": \"next1\",\n" +
                "        \"to\": \"step1\"\n" +
                "      },\n" +
                "      \"fail\": {\n" +
                "        \"on\": \"fail1\",\n" +
                "        \"exit-status\": \"x\"\n" +
                "      },\n" +
                "      \"end\": {\n" +
                "        \"on\": \"end1\",\n" +
                "        \"exit-status\": \"x\"\n" +
                "      },\n" +
                "      \"stop\": {\n" +
                "        \"on\": \"stop1\",\n" +
                "        \"exit-status\": \"x\",\n" +
                "        \"restart\": \"step1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals(1, job.getJobElements().size());
        final Flow flow = (Flow) job.getJobElements().get(0);
        assertEquals("flow1", flow.getId());

        final List<JobElement> elementsInFlow = flow.getJobElements();
        assertEquals(2, elementsInFlow.size());
        Step step = (Step) elementsInFlow.get(0);
        assertEquals("step1", step.getId());
        assertEquals("batchlet1", step.getBatchlet().getRef());
        step = (Step) elementsInFlow.get(1);
        assertEquals("step2", step.getId());
        assertEquals("batchlet2", step.getBatchlet().getRef());

        verifyTransitionElements(flow.getTransitionElements());
    }

    /**
     * Verifies a flow that contains 2 steps and transition elements:
     * end, fail, stop & next, and each of the appear twice.
     *
     * @throws Exception
     */
    @Test
    public void flowWithTransitionElements2() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"flow\": {\n" +
                "      \"id\": \"flow1\",\n" +
                "      \"step\": [\n" +
                "        {\n" +
                "          \"id\": \"step1\",\n" +
                "          \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"step2\",\n" +
                "          \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"next\": [\n" +
                "        {\n" +
                "          \"on\": \"next1\",\n" +
                "          \"to\": \"step1\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"on\": \"next2\",\n" +
                "          \"to\": \"step2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"fail\": [\n" +
                "        {\n" +
                "          \"on\": \"fail1\",\n" +
                "          \"exit-status\": \"x\"\n" +
                "        },\n" +
                "        { \"on\": \"fail2\" }\n" +
                "      ],\n" +
                "      \"end\": [\n" +
                "        {\n" +
                "          \"on\": \"end1\",\n" +
                "          \"exit-status\": \"x\"\n" +
                "        },\n" +
                "        { \"on\": \"end2\" }\n" +
                "      ],\n" +
                "      \"stop\": [\n" +
                "        {\n" +
                "          \"on\": \"stop1\",\n" +
                "          \"exit-status\": \"x\",\n" +
                "          \"restart\": \"step1\"\n" +
                "        },\n" +
                "        { \"on\": \"stop2\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        final Flow flow = (Flow) job.getJobElements().get(0);
        verifyTransitionElements2(flow.getTransitionElements());
    }

    /**
     * Verifies a step that contains one or multiple transition elements:
     * end, fail, stop & next and each of them appears twice.
     *
     * @throws Exception
     */
    @Test
    public void stepWithTransitionElements2() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"step\": [\n" +
                "      {\n" +
                "        \"id\": \"step1\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet1\" },\n" +
                "        \"next\": [\n" +
                "          {\n" +
                "            \"on\": \"next1\",\n" +
                "            \"to\": \"step1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"on\": \"next2\",\n" +
                "            \"to\": \"step2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"fail\": [\n" +
                "          {\n" +
                "            \"on\": \"fail1\",\n" +
                "            \"exit-status\": \"x\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"on\": \"fail2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"end\": [\n" +
                "          {\n" +
                "            \"on\": \"end1\",\n" +
                "            \"exit-status\": \"x\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"on\": \"end2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"stop\": [\n" +
                "          {\n" +
                "            \"on\": \"stop1\",\n" +
                "            \"exit-status\": \"x\",\n" +
                "            \"restart\": \"step1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"on\": \"stop2\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"step2\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals(2, job.getJobElements().size());
        final Step step1 = (Step) job.getJobElements().get(0);
        assertEquals("step1", step1.getId());
        verifyTransitionElements2(step1.getTransitionElements());
    }

    /**
     * Verifies that listeners and properties with only one element is processed properly.
     * It can be either job-level listeners and properties, step-level listeners and properties,
     * or artifact properties.  In this case, the JSON representation is not
     * an array.
     *
     * @throws Exception
     */
    @Test
    public void oneListenerProperty() throws Exception {
        String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"listeners\": {\n" +
                "      \"listener\": {\n" +
                "        \"ref\": \"JL1\",\n" +
                "        \"properties\": {\n" +
                "          \"property\": {\n" +
                "            \"name\": \"JLN\",\n" +
                "            \"value\": \"JLV\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"properties\": {\n" +
                "      \"property\": {\n" +
                "        \"name\": \"JN\",\n" +
                "        \"value\": \"JV\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"step\": {\n" +
                "      \"id\": \"step1\",\n" +
                "      \"listeners\": {\n" +
                "        \"listener\": {\n" +
                "          \"ref\": \"SL1\",\n" +
                "          \"properties\": {\n" +
                "            \"property\": {\n" +
                "              \"name\": \"SLN\",\n" +
                "              \"value\": \"SLV\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"properties\": {\n" +
                "        \"property\": {\n" +
                "          \"name\": \"SN\",\n" +
                "          \"value\": \"SV\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"batchlet\": {\n" +
                "        \"ref\": \"batchlet1\",\n" +
                "        \"properties\": {\n" +
                "          \"property\": {\n" +
                "            \"name\": \"BN\",\n" +
                "            \"value\": \"BV\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals(1, job.getProperties().size());
        assertEquals("JV", job.getProperties().get("JN"));
        assertEquals(1, job.getListeners().getListeners().size());
        assertEquals("JL1", job.getListeners().getListeners().get(0).getRef());
        assertEquals(1, job.getListeners().getListeners().get(0).getProperties().size());
        assertEquals("JLV", job.getListeners().getListeners().get(0).getProperties().get("JLN"));

        final Step step1 = (Step) job.getJobElements().get(0);
        assertEquals(1, step1.getProperties().size());
        assertEquals("SV", step1.getProperties().get("SN"));

        final List<RefArtifact> listeners = step1.getListeners().getListeners();
        assertEquals(1, listeners.size());
        assertEquals("SL1", listeners.get(0).getRef());
        Properties properties = listeners.get(0).getProperties();
        assertEquals(1, properties.size());
        assertEquals("SLV", properties.get("SLN"));

        final RefArtifact batchlet = step1.getBatchlet();
        assertEquals("step1", step1.getId());
        assertEquals(null, step1.getChunk());
        assertEquals("batchlet1", batchlet.getRef());
        assertEquals(1, batchlet.getProperties().size());
        assertEquals("BV", batchlet.getProperties().get("BN"));
    }

    /**
     * Verifies listeners and properties with two elements are processed properly.
     * It can be either job-level listener and properties, step-level listener and properties, or
     * artifact properties.  In this case, the JSON representation is an array.
     *
     * @throws Exception
     */
    @Test
    public void twoListenersProperties() throws Exception {
        String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"listeners\": {\n" +
                "      \"listener\": [\n" +
                "        {\n" +
                "          \"ref\": \"JL1\",\n" +
                "          \"properties\": {\n" +
                "            \"property\": [\n" +
                "              {\n" +
                "                \"name\": \"JLN\",\n" +
                "                \"value\": \"JLV\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"name\": \"JLN2\",\n" +
                "                \"value\": \"JLV2\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        { \"ref\": \"JL2\" }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"properties\": {\n" +
                "      \"property\": [\n" +
                "        {\n" +
                "          \"name\": \"JN\",\n" +
                "          \"value\": \"JV\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"JN2\",\n" +
                "          \"value\": \"JV2\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"step\": {\n" +
                "      \"id\": \"step1\",\n" +
                "      \"listeners\": {\n" +
                "        \"listener\": [\n" +
                "          {\n" +
                "            \"ref\": \"SL1\",\n" +
                "            \"properties\": {\n" +
                "              \"property\": [\n" +
                "                {\n" +
                "                  \"name\": \"SLN\",\n" +
                "                  \"value\": \"SLV\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"name\": \"SLN2\",\n" +
                "                  \"value\": \"SLV2\"\n" +
                "                }\n" +
                "              ]\n" +
                "            }\n" +
                "          },\n" +
                "          { \"ref\": \"SL2\" }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"properties\": {\n" +
                "        \"property\": [\n" +
                "          {\n" +
                "            \"name\": \"SN\",\n" +
                "            \"value\": \"SV\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"name\": \"SN2\",\n" +
                "            \"value\": \"SV2\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"batchlet\": {\n" +
                "        \"ref\": \"batchlet1\",\n" +
                "        \"properties\": {\n" +
                "          \"property\": [\n" +
                "            {\n" +
                "              \"name\": \"BN\",\n" +
                "              \"value\": \"BV\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"BN2\",\n" +
                "              \"value\": \"BV2\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals(2, job.getProperties().size());
        assertEquals("JV", job.getProperties().get("JN"));
        assertEquals("JV2", job.getProperties().get("JN2"));

        List<RefArtifact> listeners = job.getListeners().getListeners();
        assertEquals(2, listeners.size());
        assertEquals("JL1", listeners.get(0).getRef());
        assertEquals("JL2", listeners.get(1).getRef());
        assertEquals(0, listeners.get(1).getProperties().size());

        Properties properties = listeners.get(0).getProperties();
        assertEquals(2, properties.size());
        assertEquals("JLV", properties.get("JLN"));
        assertEquals("JLV2", properties.get("JLN2"));

        final Step step1 = (Step) job.getJobElements().get(0);
        assertEquals(2, step1.getProperties().size());
        assertEquals("SV", step1.getProperties().get("SN"));
        assertEquals("SV2", step1.getProperties().get("SN2"));

        listeners = step1.getListeners().getListeners();
        assertEquals(2, listeners.size());
        assertEquals("SL1", listeners.get(0).getRef());
        assertEquals("SL2", listeners.get(1).getRef());
        assertEquals(0, listeners.get(1).getProperties().size());

        properties = listeners.get(0).getProperties();
        assertEquals(2, properties.size());
        assertEquals("SLV", properties.get("SLN"));
        assertEquals("SLV2", properties.get("SLN2"));

        final RefArtifact batchlet = step1.getBatchlet();
        assertEquals("batchlet1", batchlet.getRef());
        assertEquals(2, batchlet.getProperties().size());
        assertEquals("BV", batchlet.getProperties().get("BN"));
        assertEquals("BV2", batchlet.getProperties().get("BN2"));
    }

    /**
     * Verifies a job with 2 steps and 1 decision, which in turn contains
     * next, end, fail and stop transition elements.
     *
     * @throws Exception
     */
    @Test
    public void decisionWithTransitionElements() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"step\": [\n" +
                "      {\n" +
                "        \"id\": \"step1\",\n" +
                "        \"next\": \"decision1\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"step2\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"decision\": {\n" +
                "      \"id\": \"decision1\",\n" +
                "      \"ref\": \"decider1\",\n" +
                "      \"properties\": {\n" +
                "        \"property\": {\n" +
                "          \"name\": \"DN1\",\n" +
                "          \"value\": \"DV1\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"next\": {\n" +
                "        \"on\": \"next1\",\n" +
                "        \"to\": \"step1\"\n" +
                "      },\n" +
                "      \"fail\": {\n" +
                "        \"on\": \"fail1\",\n" +
                "        \"exit-status\": \"x\"\n" +
                "      },\n" +
                "      \"end\": {\n" +
                "          \"on\": \"end1\",\n" +
                "          \"exit-status\": \"x\"\n" +
                "        },\n" +
                "      \"stop\": {\n" +
                "        \"on\": \"stop1\",\n" +
                "        \"exit-status\": \"x\",\n" +
                "        \"restart\": \"step1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        final List<JobElement> jobElements = job.getJobElements();
        assertEquals(3, jobElements.size());
        for (JobElement element : jobElements) {
            if (element instanceof Step) {
                final Step step = (Step) element;
                final String stepId = step.getId();
                if (stepId.equals("step1")) {
                    assertEquals("decision1", step.getAttributeNext());
                    assertEquals("batchlet1", step.getBatchlet().getRef());
                } else {
                    assertEquals("step2", step.getId());
                    assertEquals("batchlet2", step.getBatchlet().getRef());
                    assertEquals(null, step.getAttributeNext());
                }
            } else {
                final Decision decision = (Decision) element;
                assertEquals("decision1", decision.getId());
                assertEquals("decider1", decision.getRef());
                final Properties properties = decision.getProperties();
                assertEquals(1, properties.size());
                assertEquals("DV1", properties.get("DN1"));
                verifyTransitionElements(decision.getTransitionElements());
            }
        }
    }

    /**
     * Verifies a job with 2 steps and 2 decision, which in turn contains 2
     * next, end, fail and stop transition elements.
     *
     * @throws Exception
     */
    @Test
    public void decisionWithTransitionElements2() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"step\": [\n" +
                "      {\n" +
                "        \"id\": \"step1\",\n" +
                "        \"next\": \"decision1\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"step2\",\n" +
                "        \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"decision\": [\n" +
                "      {\n" +
                "        \"id\": \"decision1\",\n" +
                "        \"ref\": \"decider1\",\n" +
                "        \"properties\": {\n" +
                "          \"property\": [\n" +
                "            {\n" +
                "              \"name\": \"DN1\",\n" +
                "              \"value\": \"DV1\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"DN2\",\n" +
                "              \"value\": \"DV2\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        \"next\": [\n" +
                "          {\n" +
                "            \"on\": \"next1\",\n" +
                "            \"to\": \"step1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"on\": \"next2\",\n" +
                "            \"to\": \"step2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"fail\": [\n" +
                "          {\n" +
                "            \"on\": \"fail1\",\n" +
                "            \"exit-status\": \"x\"\n" +
                "          },\n" +
                "          { \"on\": \"fail2\" }\n" +
                "        ],\n" +
                "        \"end\": [\n" +
                "          {\n" +
                "            \"on\": \"end1\",\n" +
                "            \"exit-status\": \"x\"\n" +
                "          },\n" +
                "          { \"on\": \"end2\" }\n" +
                "        ],\n" +
                "        \"stop\": [\n" +
                "          {\n" +
                "            \"on\": \"stop1\",\n" +
                "            \"exit-status\": \"x\",\n" +
                "            \"restart\": \"step1\"\n" +
                "          },\n" +
                "          { \"on\": \"stop2\" }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"decision2\",\n" +
                "        \"ref\": \"decider2\",\n" +
                "        \"end\": { \"on\": \"end\" }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        final Job job = JsonJobMapper.toJob(json);
        final List<JobElement> jobElements = job.getJobElements();
        assertEquals(4, jobElements.size());
        for (JobElement element : jobElements) {
            if (element instanceof Decision) {
                if (element.getId().equals("decision1")) {
                    final Decision decision = (Decision) element;
                    assertEquals("decision1", decision.getId());
                    assertEquals("decider1", decision.getRef());
                    final Properties properties = decision.getProperties();
                    assertEquals(2, properties.size());
                    assertEquals("DV1", properties.get("DN1"));
                    assertEquals("DV2", properties.get("DN2"));
                    verifyTransitionElements2(decision.getTransitionElements());
                } else {
                    assertEquals("decision2", element.getId());
                    assertEquals("decider2", ((Decision) element).getRef());
                    assertEquals(1, element.getTransitionElements().size());
                    assertEquals("end", element.getTransitionElements().get(0).getOn());
                }
            }
        }
    }

    /**
     * Verifies various elements that can appear inside a flow:
     * 2 flows, 2 decisions, and 2 splits.
     *
     * @throws Exception
     */
    @Test
    public void flowElements() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
                "    \"flow\": {\n" +
                "      \"id\": \"flow1\",\n" +
                "      \"flow\": [\n" +
                "        {\n" +
                "          \"id\": \"flow2\",\n" +
                "          \"next\": \"flow3\",\n" +
                "          \"step\": {\n" +
                "            \"id\": \"step1\",\n" +
                "            \"batchlet\": { \"ref\": \"batchlet1\" }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"flow3\",\n" +
                "          \"step\": {\n" +
                "            \"id\": \"step2\",\n" +
                "            \"batchlet\": { \"ref\": \"batchlet2\" }\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"decision\": [\n" +
                "        {\n" +
                "          \"id\": \"decision1\",\n" +
                "          \"ref\": \"decider1\",\n" +
                "          \"end\": { \"on\": \"end\" }\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"decision2\",\n" +
                "          \"ref\": \"decider2\",\n" +
                "          \"end\": { \"on\": \"end\" }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"split\": [\n" +
                "        {\n" +
                "          \"id\": \"split1\",\n" +
                "          \"next\": \"split2\",\n" +
                "          \"flow\": [\n" +
                "            {\n" +
                "              \"id\": \"flow4\",\n" +
                "              \"step\": {\n" +
                "                \"id\": \"step3\",\n" +
                "                \"batchlet\": { \"ref\": \"batchlet3\" }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"id\": \"flow5\",\n" +
                "              \"step\": {\n" +
                "                \"id\": \"step4\",\n" +
                "                \"batchlet\": { \"ref\": \"batchlet4\" }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"split2\",\n" +
                "          \"flow\": {\n" +
                "            \"id\": \"flow6\",\n" +
                "            \"step\": {\n" +
                "              \"id\": \"step6\",\n" +
                "              \"batchlet\": { \"ref\": \"batchlet6\" }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Job job = JsonJobMapper.toJob(json);
        assertEquals(1, job.getJobElements().size());
        final Flow flow1 = (Flow) job.getJobElements().get(0);
        assertEquals("flow1", flow1.getId());

        final List<JobElement> flow1Elements = flow1.getJobElements();
        assertEquals(6, flow1Elements.size());
        int flowCount = 0;
        int decisionCount = 0;
        int splitCount = 0;

        for (JobElement e : flow1Elements) {
            final String id = e.getId();
            if (e instanceof Decision) {
                if (id.equals("decision1")) {
                    decisionCount++;
                    assertEquals("decider1", ((Decision) e).getRef());
                    assertEquals("end", e.getTransitionElements().get(0).getOn());
                } else {
                    assertEquals("decision2", id);
                    decisionCount++;
                    assertEquals("decider2", ((Decision) e).getRef());
                    assertEquals("end", e.getTransitionElements().get(0).getOn());
                }
            } else if (e instanceof Flow) {
                if (id.equals("flow2")) {
                    flowCount++;
                    final List<JobElement> flowElements = ((Flow) e).getJobElements();
                    assertEquals(1, flowElements.size());
                    assertEquals("step1", flowElements.get(0).getId());
                    assertEquals("flow3", ((Flow) e).getAttributeNext());
                } else {
                    assertEquals("flow3", id);
                    flowCount++;
                    final List<JobElement> flowElements = ((Flow) e).getJobElements();
                    assertEquals(1, flowElements.size());
                    assertEquals("step2", flowElements.get(0).getId());
                }
            } else if (e instanceof Split) {
                if (id.equals("split1")) {
                    splitCount++;
                    final Split split = (Split) e;
                    assertEquals("split2", split.getAttributeNext());
                    assertEquals(2, split.getFlows().size());
                    assertEquals("flow4", split.getFlows().get(0).getId());
                    Step step = (Step) split.getFlows().get(0).getJobElements().get(0);
                    assertEquals("step3", step.getId());
                    assertEquals("batchlet3", step.getBatchlet().getRef());

                    assertEquals("flow5", split.getFlows().get(1).getId());
                    step = (Step) split.getFlows().get(1).getJobElements().get(0);
                    assertEquals("step4", step.getId());
                    assertEquals("batchlet4", step.getBatchlet().getRef());
                } else {
                    assertEquals("split2", id);
                    splitCount++;
                    final Split split = (Split) e;
                    assertEquals(1, split.getFlows().size());
                    assertEquals("flow6", split.getFlows().get(0).getId());
                    final Step step = (Step) split.getFlows().get(0).getJobElements().get(0);
                    assertEquals("step6", step.getId());
                    assertEquals("batchlet6", step.getBatchlet().getRef());
                }
            }
        }
        assertEquals(2, decisionCount);
        assertEquals(2, flowCount);
        assertEquals(2, splitCount);
    }

    private static void verifyTransitionElements(final List<Transition> transitionElements) {
        assertEquals(4, transitionElements.size());
        int failCount = 0;
        int endCount = 0;
        int stopCount = 0;
        int nextCount = 0;

        for (Transition transition : transitionElements) {
            if (transition instanceof Transition.Fail) {
                failCount++;
                assertEquals("fail1", transition.getOn());
                assertEquals("x", ((Transition.Fail) transition).getExitStatus());
            } else if (transition instanceof Transition.End) {
                endCount++;
                assertEquals("end1", transition.getOn());
                assertEquals("x", ((Transition.End) transition).getExitStatus());
            } else if (transition instanceof Transition.Stop) {
                stopCount++;
                assertEquals("stop1", transition.getOn());
                assertEquals("x", ((Transition.Stop) transition).getExitStatus());
                assertEquals("step1", ((Transition.Stop) transition).getRestart());
            } else if(transition instanceof Transition.Next){
                nextCount++;
                assertEquals("next1", transition.getOn());
                assertEquals("step1", ((Transition.Next) transition).getTo());
            }
        }
        assertEquals(1, failCount);
        assertEquals(1, endCount);
        assertEquals(1, stopCount);
        assertEquals(1, nextCount);
    }

    private static void verifyTransitionElements2(final List<Transition> transitionElements) {
        assertEquals(8, transitionElements.size());
        int failCount = 0;
        int endCound = 0;
        int stopCount = 0;
        int nextCount = 0;
        for (Transition transition : transitionElements) {
            final String on = transition.getOn();
            if (transition instanceof Transition.End) {
                if (on.equals("end1")) {
                    endCound++;
                    assertEquals("x", ((Transition.End) transition).getExitStatus());
                } else {
                    endCound++;
                    assertEquals("end2", on);
                    assertNull(((Transition.End) transition).getExitStatus());
                }
            } else if (transition instanceof Transition.Fail) {
                if (on.equals("fail1")) {
                    failCount++;
                    assertEquals("x", ((Transition.Fail) transition).getExitStatus());
                } else {
                    failCount++;
                    assertEquals("fail2", on);
                    assertNull(((Transition.Fail) transition).getExitStatus());
                }
            } else if (transition instanceof Transition.Stop) {
                if (on.equals("stop1")) {
                    stopCount++;
                    assertEquals("x", ((Transition.Stop) transition).getExitStatus());
                    assertEquals("step1", ((Transition.Stop) transition).getRestart());
                } else {
                    stopCount++;
                    assertEquals("stop2", on);
                    assertNull(((Transition.Stop) transition).getExitStatus());
                    assertNull(((Transition.Stop) transition).getRestart());
                }
            } else if (transition instanceof Transition.Next) {
                if (on.equals("next1")) {
                    nextCount++;
                    assertEquals("next1", on);
                    assertEquals("step1", ((Transition.Next) transition).getTo());
                } else {
                    nextCount++;
                    assertEquals("next2", on);
                    assertEquals("step2", ((Transition.Next) transition).getTo());
                }
            } else {
                throw new IllegalStateException("Unexpected transition element: " + transition);
            }
        }
        assertEquals(2, failCount);
        assertEquals(2, endCound);
        assertEquals(2, stopCount);
        assertEquals(2, nextCount);
    }
}
