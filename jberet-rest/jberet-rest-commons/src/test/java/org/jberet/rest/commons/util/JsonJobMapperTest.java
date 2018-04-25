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
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Properties;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Step;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        assertEquals("arrayItemReader",reader.getRef());
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
     * @throws Exception
     */
    @Test
    public void twoSteps() throws Exception {
        final String json = "{\n" +
                "  \"job\": {\n" +
                "    \"id\": \"job1\",\n" +
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
}
