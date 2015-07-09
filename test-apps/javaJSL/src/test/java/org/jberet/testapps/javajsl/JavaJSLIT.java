/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.javajsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.jberet.job.model.DecisionBuilder;
import org.jberet.job.model.FlowBuilder;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.SplitBuilder;
import org.jberet.job.model.StepBuilder;
import org.jberet.runtime.metric.StepMetrics;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class JavaJSLIT extends AbstractIT {
    static final String batchlet1Name = "batchlet1";
    static final String deciderName = "decider2";

    /**
     * Creates a job with Java JSL:
     * add 2 job properties;
     * add 1 job listener that has 2 batch properties;
     * add 1 step that has
     * 2 step properties;
     * 1 batchlet that has 2 batch properties;
     * 1 step listener that has 2 batch properties;
     * stop transition element;
     * end transition element;
     * fail transition element;
     * next transition element;
     * <p/>
     * add another step that has
     * 1 batchlet.
     * <p/>
     * Job or step properties can be set one by one, or set multiple properties together with either a series of String[]
     * or java.util.Properties.
     * <p/>
     * Batch artifacts can be created along with its batch properties in the form of either a series of String[], or
     * java.util.Properties.  When using String[] to specify a property, the first element is key and the second element
     * is its value.
     *
     * @throws Exception
     */
    @Test
    public void batchlet1() throws Exception {
        final String jobName = "javaJSL-batchlet1";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";

        final Properties stepListenerProps = new Properties();
        stepListenerProps.setProperty("stepListenerk1", "l");
        stepListenerProps.setProperty("stepListenerk2", "l");

        //used to test property resolution
        params.setProperty("jobListenerPropVal", "L");

        final Job job = new JobBuilder(jobName)
                .restartable(false)
                .property("jobk1", "J")
                .property("jobk2", "J")
                .listener("jobListener1", new String[]{"jobListenerk1", "#{jobParameters['jobListenerPropVal']}"},
                        new String[]{"jobListenerk2", "#{jobParameters['jobListenerPropVal']}"})
                .step(new StepBuilder(stepName)
                        .properties(new String[]{"stepk1", "S"},
                                new String[]{"stepk2", "S"})
                        .batchlet(batchlet1Name, new String[]{"batchletk1", "B"},
                                new String[]{"batchletk2", "B"})
                        .listener("stepListener1", stepListenerProps)
                        .stopOn("STOP").restartFrom(stepName).exitStatus()
                        .endOn("END").exitStatus("new status for end")
                        .failOn("FAIL").exitStatus()
                        .nextOn("*").to(step2Name)
                        .build())
                .step(new StepBuilder(step2Name)
                        .batchlet(batchlet1Name).build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals("LL", jobExecution.getExitStatus());
        Assert.assertEquals("JJSSBBll", stepExecution0.getExitStatus());
    }

    /**
     * Runs a chunk step with item reader, processor, writer, chunk listener, and verifies the result step metrics.
     * {@link ChunkListener1} sets the step exit status to {@code stepExitStatusExpected} in its {@code afterChunk}
     * method, and expects the resulting step exit status to contain that string value.
     * <p/>
     * Note that {@link StepBuilder} does not have a chunk(...) method to match the XML i element.
     * You just directly configure all chunk related attributes and sub-elements in {@code StepBuilder}.
     *
     * @throws Exception
     */
    @Test
    public void chunk1() throws Exception {
        final String jobName = "javaJSL-chunk1";
        final String stepName = jobName + ".step1";
        final String stepExitStatusExpected = "stepExitStatusExpected";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
                        .processor("integerProcessor")
                        .checkpointPolicy("item")
                        .listener("chunkListener1", new String[]{"stepExitStatus", stepExitStatusExpected})
                        .itemCount(10)
                        .allowStartIfComplete()
                        .startLimit(2)
                        .skipLimit(8)
                        .timeLimit(2, TimeUnit.MINUTES)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("step exit status: %s%n", stepExecution0.getExitStatus());
        Assert.assertEquals(true, stepExecution0.getExitStatus().contains(stepExitStatusExpected));

        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(4, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Runs a partitioned chunk step with item reader, processor, writer, and verifies the result step metrics.
     * The partition is configured with a partition plan.
     * <p/>
     * Note that {@link StepBuilder} does not have a partition method matching the {@code partition} XML element.
     * All partition configurations are specified directly under {@code StepBuilder}.
     *
     * @throws Exception
     */
    @Test
    public void chunkPartitionPlan() throws Exception {
        final String jobName = "javaJSL-chunkPartition";
        final String stepName = jobName + ".step1";

        final int partitionCount = 3;
        final List<Properties> listOfPartitionProps = new ArrayList<Properties>();
        for (int i = 0; i < partitionCount; i++) {
            listOfPartitionProps.add(new Properties());
        }
        listOfPartitionProps.get(0).put("partition.start", "0");
        listOfPartitionProps.get(0).put("partition.end", "9");

        listOfPartitionProps.get(1).put("partition.start", "10");
        listOfPartitionProps.get(1).put("partition.end", "19");

        listOfPartitionProps.get(2).put("partition.start", "20");
        listOfPartitionProps.get(2).put("partition.end", "29");


        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"},
                                new String[]{"partition.start", "#{partitionPlan['partition.start']}"},
                                new String[]{"partition.end", "#{partitionPlan['partition.end']}"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
                        .partitionPlan(partitionCount, listOfPartitionProps)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(6, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Runs a partitioned chunk step with item reader, processor, writer, and verifies the result step metrics.
     * The partition is configured with {@link PartitionMapper1}
     *
     * @throws Exception
     */
    @Test
    public void chunkPartitionMapper() throws Exception {
        final String jobName = "javaJSL-chunkPartitionMapper";
        final String stepName = jobName + ".step1";
        final int partitionCount = 3;

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"},
                                new String[]{"partition.start", "#{partitionPlan['partition.start']}"},
                                new String[]{"partition.end", "#{partitionPlan['partition.end']}"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
                        .partitionMapper("partitionMapper1", new String[]{"partitionCount", String.valueOf(partitionCount)})
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(6, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Runs a partitioned chunk step with item reader, processor, writer, partition collector, analyzer and reducer,
     * and verifies the result step exit status.
     *
     * @throws Exception
     */
    @Test
    public void chunkPartitionCollectorAnalyzerReducer() throws Exception {
        final String jobName = "javaJSL-chunkPartitionCollectorAnalyzerReducer";
        final String stepName = jobName + ".step1";
        final int partitionCount = 3;

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"},
                                new String[]{"partition.start", "#{partitionPlan['partition.start']}"},
                                new String[]{"partition.end", "#{partitionPlan['partition.end']}"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
                        .partitionMapper("partitionMapper1", new String[]{"partitionCount", String.valueOf(partitionCount)})
                        .partitionCollector("partitionCollector1")
                        .partitionAnalyzer("partitionAnalyzer1")
                        .partitionReducer("partitionReducer1")
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("step exit status: %s%n", stepExecution0.getExitStatus());

        // collector * 2 chunk * 3 partitions + analyzer * 3 = 9
        Assert.assertEquals("9", stepExecution0.getExitStatus());
    }

    /**
     * Runs a chunk step with item reader, processor, writer, and skip-exception-classes,
     * and verifies the result step metrics.
     *
     * @throws Exception
     */
    @Test
    public void skipExceptions() throws Exception {
        final String jobName = "javaJSL-skipExceptions";
        final String stepName = jobName + ".step1";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "29"}, new String[]{"writer.sleep.time", "0"})
                        .processor("integerProcessor")
                        .skippableExceptionsInclude(ArithmeticException.class, IllegalFormatCodePointException.class)
                        .skippableExceptionsExclude(IOException.class, FileNotFoundException.class)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(1, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(3, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(20, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Runs a chunk step with item reader, processor, writer, and retry-exception-classes,
     * and verifies the result step metrics.
     *
     * @throws Exception
     */
    @Test
    public void retryExceptions() throws Exception {
        final String jobName = "javaJSL-retryExceptions";
        final String stepName = jobName + ".step1";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "29"}, new String[]{"writer.sleep.time", "0"})
                        .processor("integerProcessor")
                        .retryableExceptionsInclude(ArithmeticException.class, IllegalFormatCodePointException.class)
                        .retryableExceptionsExclude(IOException.class, FileNotFoundException.class)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(13, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(40, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(1, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Runs a chunk step with item reader, processor, writer, retry-exception-classes, and no-rollback-exception-classes
     * and verifies the result step metrics.
     *
     * @throws Exception
     */
    @Test
    public void retryExceptionsAndNoRollbackExceptions() throws Exception {
        final String jobName = "javaJSL-retryExceptionsAndNoRollbackExceptions";
        final String stepName = jobName + ".step1";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "29"}, new String[]{"writer.sleep.time", "0"})
                        .processor("integerProcessor")
                        .retryableExceptionsInclude(ArithmeticException.class, IllegalFormatCodePointException.class)
                        .retryableExceptionsExclude(IOException.class, FileNotFoundException.class)
                        .noRollbackExceptionsInclude(ArithmeticException.class, IllegalStateException.class)
                        .noRollbackExceptionsExclude(IOException.class, FileNotFoundException.class)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(4, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Runs a chunk step with item reader, processor, writer, and verifies the result step metrics.
     *
     * @throws Exception
     * @see {@link #chunk1()}
     */
    @Test
    public void checkpointAlgorithm() throws Exception {
        final String jobName = "javaJSL-checkpointAlgorithm";
        final String stepName = jobName + ".step1";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .reader("integerArrayReader", new String[]{"data.count", "30"})
                        .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
                        .processor("integerProcessor")
                        .checkpointPolicy("custom")
                        .checkpointAlgorithm("checkpointAlgorithm1")
                        .startLimit(2)
                        .skipLimit(8)
                        .retryLimit(10)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        final Metric[] metrics = stepExecution0.getMetrics();
        System.out.printf("metrics: %s%n", java.util.Arrays.asList(metrics));
        final StepMetrics stepMetrics = stepExecution0.getStepMetrics();
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.WRITE_SKIP_COUNT));
        Assert.assertEquals(31, stepMetrics.get(Metric.MetricType.COMMIT_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.READ_COUNT));
        Assert.assertEquals(30, stepMetrics.get(Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.READ_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.PROCESS_SKIP_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.ROLLBACK_COUNT));
        Assert.assertEquals(0, stepMetrics.get(Metric.MetricType.FILTER_COUNT));
    }

    /**
     * Builds a job consisting of 1 flow, which consists of 2 steps.
     *
     * @throws Exception
     */
    @Test
    public void flow1() throws Exception {
        final String jobName = "javaJSL-flow1";
        final String flowName = jobName + "flow1";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";

        final Job job = new JobBuilder(jobName)
                .restartable()
                .flow(new FlowBuilder(flowName)
                        .step(new StepBuilder(stepName).batchlet(batchlet1Name)
                                .next(step2Name)
                                .build())
                        .step(new StepBuilder(step2Name).batchlet(batchlet1Name)
                                .build())
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(2, stepExecutions.size());
        Assert.assertEquals(stepName, stepExecution0.getStepName());
        Assert.assertEquals(step2Name, stepExecutions.get(1).getStepName());
    }

    /**
     * Builds a job consisting of 1 step and  1 decision.
     *
     * @throws Exception
     */
    @Test
    public void decision1() throws Exception {
        final String jobName = "javaJSL-decision1";
        final String stepName = jobName + ".step1";
        final String decisionName = jobName + ".decision1";

        final Job job = new JobBuilder(jobName)
                .restartable(true)
                .step(new StepBuilder(stepName).batchlet(batchlet1Name).next(decisionName)
                        .build())
                .decision(new DecisionBuilder(decisionName, deciderName)
                        .failOn("FAIL").exitStatus()
                        .stopOn("STOP").restartFrom(stepName).exitStatus()
                        .nextOn("NEXT").to(stepName)
                        .endOn("*").exitStatus(stepName)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(1, stepExecutions.size());
        Assert.assertEquals(stepName, stepExecution0.getStepName());
        Assert.assertEquals(stepName, jobExecution.getExitStatus());  //set by the decision element endOn("*").exitStatus(...)
    }

    /**
     * Builds a job consisting of 1 split and 1 step. The split consists of 2 flows, each of which consists of 1 step.
     * Altogether 3 steps.
     *
     * @throws Exception
     */
    @Test
    public void split1() throws Exception {
        final String jobName = "javaJSL-split1";
        final String splitName = jobName + ".split1";
        final String flowName = splitName + ".flow1";
        final String flow2Name = splitName + ".flow2";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";
        final String step3Name = jobName + ".step3";

        final Job job = new JobBuilder(jobName)
                .split(new SplitBuilder(splitName)
                        .flow(new FlowBuilder(flowName)
                                .step(new StepBuilder(stepName).batchlet(batchlet1Name).build())
                                .build())
                        .flow(new FlowBuilder(flow2Name)
                                .step(new StepBuilder(step2Name).batchlet(batchlet1Name).build())
                                .build())
                        .next(step3Name)
                        .build())
                .step(new StepBuilder(step3Name).batchlet(batchlet1Name)
                        .endOn("*").exitStatus(step3Name)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(3, stepExecutions.size());

        //step1 and step2 execution order may be random, so stepExecution0 may point to step1 or step2
        //Assert.assertEquals(stepName, stepExecution0.getStepName());
        //Assert.assertEquals(step2Name, stepExecutions.get(1).getStepName());
        Assert.assertEquals(step3Name, stepExecutions.get(2).getStepName());
    }

    @Test(expected = BatchRuntimeException.class)
    public void duplicateStepId() throws Exception {
        final String jobName = "javaJSL-duplicateStepId";
        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(jobName)
                        .batchlet(batchlet1Name)
                        .build())
                .build();
        startJob(job);
    }

    @Test(expected = BatchRuntimeException.class)
    public void duplicateDecisionId() throws Exception {
        final String jobName = "javaJSL-duplicateDecisionId";
        final String stepName = jobName + ".step1";
        final String decisionName = stepName;
        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName)
                        .batchlet(batchlet1Name)
                        .next(decisionName)
                        .build())
                .decision(new DecisionBuilder(decisionName, "decider2")
                        .endOn("*").exitStatus()
                        .build())
                .build();
        startJob(job);
    }

    @Test(expected = BatchRuntimeException.class)
    public void duplicateFlowId() throws Exception {
        final String jobName = "javaJSL-duplicateFlowId";
        final String flowName = jobName + "flow1";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName;

        final Job job = new JobBuilder(jobName)
                .flow(new FlowBuilder(flowName)
                        .step(new StepBuilder(stepName).batchlet(batchlet1Name)
                                .next(step2Name)
                                .build())
                        .step(new StepBuilder(step2Name).batchlet(batchlet1Name)
                                .build())
                        .build())
                .build();

        startJob(job);
    }

    @Test(expected = BatchRuntimeException.class)
    public void duplicateSplitId() throws Exception {
        final String jobName = "javaJSL-duplicateSplitId";
        final String splitName = jobName + ".split1";
        final String flowName = splitName + ".flow1";
        final String flow2Name = splitName + ".flow2";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";
        final String step3Name = flowName;

        final Job job = new JobBuilder(jobName)
                .split(new SplitBuilder(splitName)
                        .flow(new FlowBuilder(flowName)
                                .step(new StepBuilder(stepName).batchlet(batchlet1Name).build())
                                .build())
                        .flow(new FlowBuilder(flow2Name)
                                .step(new StepBuilder(step2Name).batchlet(batchlet1Name).build())
                                .build())
                        .next(step3Name)
                        .build())
                .step(new StepBuilder(step3Name).batchlet(batchlet1Name)
                        .endOn("*").exitStatus(step3Name)
                        .build())
                .build();

        startJob(job);
    }

    @Test
    public void noClassDefFoundErrorFromBatchlet() throws Exception {
        final String jobName = "javaJSL-noClassDefFoundErrorFromBatchlet";
        final String stepName = jobName + ".step1";
        final String batchletName = "batchletWithNoClassDefFoundError";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName).batchlet(batchletName).partitionPlan(2).build())
                .build();

        startJobAndWait(job);
        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void noClassDefFoundErrorFromItemReader() throws Exception {
        final String jobName = "javaJSL-noClassDefFoundErrorFromItemReader";
        final String stepName = jobName + ".step1";
        final String itemReaderName = "itemReaderWithNoClassDefFoundError";
        final String itemWriterName = "itemWriterWithNoClassDefFoundError";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName).reader(itemReaderName, new String[]{"throwError", "true"})
                        .writer(itemWriterName)
                        .partitionPlan(2).build())
                .build();

        startJobAndWait(job);
        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void noClassDefFoundErrorFromItemWriter() throws Exception {
        final String jobName = "javaJSL-noClassDefFoundErrorFromItemWriter";
        final String stepName = jobName + ".step1";
        final String itemReaderName = "itemReaderWithNoClassDefFoundError";
        final String itemWriterName = "itemWriterWithNoClassDefFoundError";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName).reader(itemReaderName)
                        .writer(itemWriterName, new String[]{"throwError", "true"})
                        .partitionPlan(2).build())
                .build();

        startJobAndWait(job);
        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void noClassDefFoundErrorFromItemProcessor() throws Exception {
        final String jobName = "javaJSL-noClassDefFoundErrorFromItemProcessor";
        final String stepName = jobName + ".step1";
        final String itemReaderName = "itemReaderWithNoClassDefFoundError";
        final String itemWriterName = "itemWriterWithNoClassDefFoundError";
        final String itemProcessorName = "itemProcessorWithNoClassDefFoundError";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName).reader(itemReaderName).writer(itemWriterName)
                        .processor(itemProcessorName, new String[]{"throwError", "true"})
                        .partitionPlan(2).build())
                .build();

        startJobAndWait(job);
        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void noClassDefFoundErrorFromChunkListener() throws Exception {
        final String jobName = "javaJSL-noClassDefFoundErrorFromChunkListener";
        final String stepName = jobName + ".step1";
        final String itemReaderName = "itemReaderWithNoClassDefFoundError";
        final String itemWriterName = "itemWriterWithNoClassDefFoundError";
        final String chunkListenerName = "chunkListenerWithNoClassDefFoundError";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName).reader(itemReaderName).writer(itemWriterName)
                        .listener(chunkListenerName, new String[]{"throwError", "true"})
                        .partitionPlan(2)
                        .build())
                .build();

        startJobAndWait(job);
        Assert.assertEquals(BatchStatus.FAILED, stepExecution0.getBatchStatus());
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

}
