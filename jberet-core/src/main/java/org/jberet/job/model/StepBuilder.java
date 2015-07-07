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

package org.jberet.job.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class StepBuilder extends AbstractPropertiesBuilder<StepBuilder> {
    private final String id;
    private int startLimit;
    private boolean allowStartIfComplete;
    private String next;
    private Listeners listeners;
    private RefArtifact batchlet;
    private final Chunk chunk = new Chunk();
    private final Partition partition = new Partition();
    private final List<Transition> transitions = new ArrayList<Transition>();

    public StepBuilder(final String id) {
        this.id = id;
    }

    public StepBuilder startLimit(final int i) {
        this.startLimit = i;
        return this;
    }

    public StepBuilder allowStartIfComplete(final boolean... b) {
        if (b.length == 0) {
            allowStartIfComplete = true;
        } else {
            allowStartIfComplete = b[0];
        }
        return this;
    }

    public StepBuilder next(final String next) {
        this.next = next;
        return this;
    }

    public StepBuilder listener(final String listenerRef, final String[]... pairsOfKeyValue) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(JobBuilder.createRefArtifactWithProperties(listenerRef, null, pairsOfKeyValue));
        return this;
    }

    public StepBuilder listener(final String listenerRef, final java.util.Properties props) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(JobBuilder.createRefArtifactWithProperties(listenerRef, props));
        return this;
    }

    public StepBuilder batchlet(final String batchletRef, final String[]... pairsOfKeyValue) {
        batchlet = JobBuilder.createRefArtifactWithProperties(batchletRef, null, pairsOfKeyValue);
        return this;
    }

    public StepBuilder batchlet(final String batchletRef, final java.util.Properties props) {
        batchlet = JobBuilder.createRefArtifactWithProperties(batchletRef, props);
        return this;
    }


    public Transition.End<StepBuilder> endOn(final String exitStatus) {
        final Transition.End<StepBuilder> end = new Transition.End<StepBuilder>(exitStatus);
        end.enclosingBuilder = this;
        transitions.add(end);
        return end;
    }

    public Transition.Fail<StepBuilder> failOn(final String exitStatus) {
        final Transition.Fail<StepBuilder> fail = new Transition.Fail<StepBuilder>(exitStatus);
        fail.enclosingBuilder = this;
        transitions.add(fail);
        return fail;
    }

    public Transition.Stop<StepBuilder> stopOn(final String exitStatus) {
        final Transition.Stop<StepBuilder> stop = new Transition.Stop<StepBuilder>(exitStatus, null);
        stop.enclosingBuilder = this;
        transitions.add(stop);
        return stop;
    }

    public Transition.Next<StepBuilder> nextOn(final String exitStatus) {
        final Transition.Next<StepBuilder> nx = new Transition.Next<StepBuilder>(exitStatus);
        nx.enclosingBuilder = this;
        transitions.add(nx);
        return nx;
    }


    public StepBuilder partitionMapper(final String partitionMapperRef, final String[]... pairsOfKeyValue) {
        partition.setMapper(JobBuilder.createRefArtifactWithProperties(partitionMapperRef, null, pairsOfKeyValue));
        return this;
    }

    public StepBuilder partitionMapper(final String partitionMapperRef, final java.util.Properties props) {
        partition.setMapper(JobBuilder.createRefArtifactWithProperties(partitionMapperRef, props));
        return this;
    }

    public StepBuilder partitionCollector(final String partitionCollectorRef, final String[]... pairsOfKeyValue) {
        partition.setCollector(JobBuilder.createRefArtifactWithProperties(partitionCollectorRef, null, pairsOfKeyValue));
        return this;
    }

    public StepBuilder partitionCollector(final String partitionCollectorRef, final java.util.Properties props) {
        partition.setCollector(JobBuilder.createRefArtifactWithProperties(partitionCollectorRef, props));
        return this;
    }

    public StepBuilder partitionAnalyzer(final String partitionAnalyzerRef, final String[]... pairsOfKeyValue) {
        partition.setAnalyzer(JobBuilder.createRefArtifactWithProperties(partitionAnalyzerRef, null, pairsOfKeyValue));
        return this;
    }

    public StepBuilder partitionAnalyzer(final String partitionAnalyzerRef, final java.util.Properties props) {
        partition.setAnalyzer(JobBuilder.createRefArtifactWithProperties(partitionAnalyzerRef, props));
        return this;
    }

    public StepBuilder partitionReducer(final String partitionReducerRef, final String[]... pairsOfKeyValue) {
        partition.setReducer(JobBuilder.createRefArtifactWithProperties(partitionReducerRef, null, pairsOfKeyValue));
        return this;
    }

    public StepBuilder partitionReducer(final String partitionReducerRef, final java.util.Properties props) {
        partition.setReducer(JobBuilder.createRefArtifactWithProperties(partitionReducerRef, props));
        return this;
    }

    public StepBuilder partitionPlan(final int partitionCount, final int threadCount, final List<java.util.Properties> listOfPartitionProps) {
        final PartitionPlan plan = new PartitionPlan();
        plan.setPartitions(String.valueOf(partitionCount));
        plan.setThreads(String.valueOf(threadCount));
        if (listOfPartitionProps != null) {
            for (int i = 0; i < listOfPartitionProps.size(); ++i) {
                final java.util.Properties juprops = listOfPartitionProps.get(i);
                final Properties props = new Properties();
                props.setPartition(String.valueOf(i));
                for (final String k : juprops.stringPropertyNames()) {
                    props.add(k, juprops.getProperty(k));
                }
                plan.addProperties(props);
            }
        }
        partition.setPlan(plan);
        return this;
    }

    public StepBuilder partitionPlan(final int partitionCount, final List<java.util.Properties> listOfPartitionProps) {
        return partitionPlan(partitionCount, partitionCount, listOfPartitionProps);
    }

    public StepBuilder partitionPlan(final int partitionCount, final int... threadCount) {
        return partitionPlan(partitionCount, threadCount.length == 0 ? partitionCount : threadCount[0], null);
    }


    public StepBuilder reader(final String readerRef, final String[]... pairsOfKeyValue) {
        chunk.reader = JobBuilder.createRefArtifactWithProperties(readerRef, null, pairsOfKeyValue);
        return this;
    }

    public StepBuilder reader(final String readerRef, final java.util.Properties props) {
        chunk.reader = JobBuilder.createRefArtifactWithProperties(readerRef, props);
        return this;
    }

    public StepBuilder writer(final String writerRef, final String[]... pairsOfKeyValue) {
        chunk.writer = JobBuilder.createRefArtifactWithProperties(writerRef, null, pairsOfKeyValue);
        return this;
    }

    public StepBuilder writer(final String writerRef, final java.util.Properties props) {
        chunk.writer = JobBuilder.createRefArtifactWithProperties(writerRef, props);
        return this;
    }

    public StepBuilder processor(final String processorRef, final String[]... pairsOfKeyValue) {
        chunk.processor = JobBuilder.createRefArtifactWithProperties(processorRef, null, pairsOfKeyValue);
        return this;
    }

    public StepBuilder processor(final String processorRef, final java.util.Properties props) {
        chunk.processor = JobBuilder.createRefArtifactWithProperties(processorRef, props);
        return this;
    }

    public StepBuilder checkpointAlgorithm(final String checkpointAlgorithmRef, final String[]... pairsOfKeyValue) {
        chunk.checkpointAlgorithm = JobBuilder.createRefArtifactWithProperties(checkpointAlgorithmRef, null, pairsOfKeyValue);
        return this;
    }

    public StepBuilder checkpointAlgorithm(final String checkpointAlgorithmRef, final java.util.Properties props) {
        chunk.checkpointAlgorithm = JobBuilder.createRefArtifactWithProperties(checkpointAlgorithmRef, props);
        return this;
    }

    public StepBuilder checkpointPolicy(final String checkpointPolicy) {
        chunk.checkpointPolicy = checkpointPolicy;
        return this;
    }

    public StepBuilder itemCount(final int itemCount) {
        chunk.itemCount = String.valueOf(itemCount);
        return this;
    }

    public StepBuilder timeLimit(final int timeLimit, final TimeUnit... timeUnit) {
        if (timeUnit.length == 0) {
            chunk.timeLimit = String.valueOf(timeLimit);
        } else {
            chunk.timeLimit = String.valueOf(TimeUnit.SECONDS.convert(timeLimit, timeUnit[0]));
        }
        return this;
    }

    public StepBuilder skipLimit(final int skipLimit) {
        chunk.skipLimit = String.valueOf(skipLimit);
        return this;
    }

    public StepBuilder retryLimit(final int retryLimit) {
        chunk.retryLimit = String.valueOf(retryLimit);
        return this;
    }


    public StepBuilder skippableExceptionsInclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.skippableExceptionClasses == null) {
            chunk.skippableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.skippableExceptionClasses.include.add(cl.getName());
        }
        return this;
    }

    public StepBuilder skippableExceptionsExclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.skippableExceptionClasses == null) {
            chunk.skippableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.skippableExceptionClasses.exclude.add(cl.getName());
        }
        return this;
    }

    public StepBuilder retryableExceptionsInclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.retryableExceptionClasses == null) {
            chunk.retryableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.retryableExceptionClasses.include.add(cl.getName());
        }
        return this;
    }

    public StepBuilder retryableExceptionsExclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.retryableExceptionClasses == null) {
            chunk.retryableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.retryableExceptionClasses.exclude.add(cl.getName());
        }
        return this;
    }

    public StepBuilder noRollbackExceptionsInclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.noRollbackExceptionClasses == null) {
            chunk.noRollbackExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.noRollbackExceptionClasses.include.add(cl.getName());
        }
        return this;
    }

    public StepBuilder noRollbackExceptionsExclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.noRollbackExceptionClasses == null) {
            chunk.noRollbackExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.noRollbackExceptionClasses.exclude.add(cl.getName());
        }
        return this;
    }


    public Step build() {
        final Step step = new Step(id);
        step.startLimit = String.valueOf(startLimit);
        step.allowStartIfComplete = String.valueOf(allowStartIfComplete);
        step.next = next;
        step.setListeners(listeners);

        if (nameValues.size() > 0) {
            step.setProperties(nameValuesToProperties(nameValues));
        }
        if (chunk.reader != null) {
            step.chunk = chunk;
        }
        step.batchlet = batchlet;

        if (partition.getPlan() != null || partition.getMapper() != null) {
            step.partition = partition;
        }
        step.getTransitionElements().addAll(transitions);
        return step;
    }
}
