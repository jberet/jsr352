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

/**
 * Builder class for building a single {@linkplain Step step}. After the step is built, the same {@code StepBuilder}
 * instance should not be reused to build another step.
 * <p/>
 * This class tries to model the {@code jsl:Step} element in job XML, while keeping a somewhat flattened structure.
 * Methods for setting step sub-elements resides directly under {@code StepBuilder} where possible, avoiding the need
 * for drilling down to sub-elements and popping back to parent context. For instance,
 * <p/>
 * <ul>
 * <li>batchlet-specific method, {@link #batchlet(String, java.util.Properties)} and
 * {@link #batchlet(String, java.util.Properties)} are directly in {@code StepBuilder};
 * <li>chunk-specific method, {@link #reader(String, java.util.Properties)}, {@link #processor(String, java.util.Properties)},
 * {@link #writer(java.lang.String, java.util.Properties)}, etc are directly in {@code StepBuilder}, with no intermediary chunk builder;
 * <li>partition-specific method, {@link #partitionPlan(int, int, List)}, {@link #partitionMapper(java.lang.String, java.util.Properties)},
 * {@link #partitionReducer(String, java.util.Properties)}, {@link #partitionCollector(java.lang.String, java.util.Properties)}, etc
 * are directly in {@code StepBuilder}, with no intermediary partition builder.
 * </ul>
 * <p/>
 * However, transition methods, such as {@link #endOn(String)}, {@link #stopOn(String)}, {@link #failOn(String)},
 * and {@link #nextOn(String)} will drill down to {@link org.jberet.job.model.Transition.End},
 * {@link org.jberet.job.model.Transition.Stop}, {@link org.jberet.job.model.Transition.Fail},
 * and {@link org.jberet.job.model.Transition.Next} respectively. These classes all contain a terminating method, which
 * pops the context back to the current {@code StepBuilder}.
 * <p/>
 * This class does not support multi-threaded access or modification. Usage example,
 * <p/>
 * <pre>
 *     Step step1 = new StepBuilder(step1Name).batchlet(batchlet1Name).build();
 *
 *     Step step2 = new StepBuilder(step2Name)
 *              .properties(new String[]{"stepk1", "S"}, new String[]{"stepk2", "S"})
 *              .batchlet(batchlet1Name, new String[]{"batchletk1", "B"}, new String[]{"batchletk2", "B"})
 *              .listener("stepListener1", stepListenerProps)
 *              .stopOn("STOP").restartFrom(step1Name).exitStatus()
 *              .endOn("END").exitStatus("new status for end")
 *              .failOn("FAIL").exitStatus()
 *              .nextOn("*").to(step3Name)
 *              .build());
 *
 *     Step step3 = new StepBuilder(step3Name)
 *              .reader("integerArrayReader", new String[]{"data.count", "30"})
 *              .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
 *              .processor("integerProcessor")
 *              .checkpointPolicy("item")
 *              .listener("chunkListener1", new String[]{"stepExitStatus", stepExitStatusExpected})
 *              .itemCount(10)
 *              .allowStartIfComplete()
 *              .startLimit(2)
 *              .skipLimit(8)
 *              .timeLimit(2, TimeUnit.MINUTES)
 *              .build());
 *
 *     Step step4 = new StepBuilder(stepName)
 *              .reader("integerArrayReader", new String[]{"data.count", "30"},
 *                      new String[]{"partition.start", "#{partitionPlan['partition.start']}"},
 *                      new String[]{"partition.end", "#{partitionPlan['partition.end']}"})
 *              .writer("integerArrayWriter", new String[]{"fail.on.values", "-1"}, new String[]{"writer.sleep.time", "0"})
 *              .partitionMapper("partitionMapper1", new String[]{"partitionCount", String.valueOf(partitionCount)})
 *              .partitionCollector("partitionCollector1")
 *              .partitionAnalyzer("partitionAnalyzer1")
 *              .partitionReducer("partitionReducer1")
 *              .build())
 * </pre>
 *
 * @see JobBuilder
 * @see FlowBuilder
 * @see SplitBuilder
 * @see DecisionBuilder
 *
 * @since 1.2.0
 */
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

    /**
     * Constructs a {@code StepBuilder} for building the step with the specified {@code id}.
     *
     * @param id the step id, corresponding to the id attribute of jsl:Step element in XML
     */
    public StepBuilder(final String id) {
        this.id = id;
    }

    /**
     * Sets the {@code start-limit} attribute value for the step.
     *
     * @param i {@code start-limit} value
     * @return this {@code StepBuilder}
     */
    public StepBuilder startLimit(final int i) {
        this.startLimit = i;
        return this;
    }

    /**
     * Sets the {@code allow-start-if-complete} attribute value for the step. This method may be invoked with 0 or 1
     * boolean value. Invoking {@code allowStartIfComplete()} is equivalent to {@code allowStartIfComplete(true)}.
     *
     * @param b optional boolean value for {@code allow-start-if-complete} attribute
     * @return this {@code StepBuilder}
     */
    public StepBuilder allowStartIfComplete(final boolean... b) {
        if (b.length == 0) {
            allowStartIfComplete = true;
        } else {
            allowStartIfComplete = b[0];
        }
        return this;
    }

    /**
     * Sets the {@code next} attribute value for the step.
     *
     * @param next {@code next} attribute value
     * @return  this {@code StepBuilder}
     */
    public StepBuilder next(final String next) {
        this.next = next;
        return this;
    }

    /**
     * Adds a step listener to the step. The listener may be added with 0 or more listener properties. Each listener
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * listener("listener1");
     * listener1("listener2", new String[]{"key1", "value1"});
     * listener1("listener3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * listener1("listener4", new String[]{"stepListenerk1", "#{jobParameters['stepListenerPropVal']}"}
     * </pre>
     *
     * @param listenerRef step listener name
     * @param pairsOfKeyValue optional listener properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder listener(final String listenerRef, final String[]... pairsOfKeyValue) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(JobBuilder.createRefArtifactWithProperties(listenerRef, null, pairsOfKeyValue));
        return this;
    }

    /**
     * Adds a step listener to the step, with listener properties.
     *
     * @param listenerRef step listener name
     * @param props step listener properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder listener(final String listenerRef, final java.util.Properties props) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(JobBuilder.createRefArtifactWithProperties(listenerRef, props));
        return this;
    }

    /**
     * Sets the {@code batchlet} for the step. The batchlet may carry 0 or more properties. Each batchlet
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * batchlet("batchlet1");
     * batchlet1("batchlet2", new String[]{"key1", "value1"});
     * batchlet1("batchlet3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * batchlet1("batchlet4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param batchletRef batchlet name
     * @param pairsOfKeyValue optional batchlet properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder batchlet(final String batchletRef, final String[]... pairsOfKeyValue) {
        batchlet = JobBuilder.createRefArtifactWithProperties(batchletRef, null, pairsOfKeyValue);
        return this;
    }

    /**
     * Sets the {@code batchlet} for the step, with batchlet properties.
     *
     * @param batchletRef batchlet name
     * @param props batchlet properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder batchlet(final String batchletRef, final java.util.Properties props) {
        batchlet = JobBuilder.createRefArtifactWithProperties(batchletRef, props);
        return this;
    }

    /**
     * Sets {@code end} transition condition for the step. This method does NOT return the current {@code StepBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.End}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.End#exitStatus(String...)} will end the operation on
     * {@code Transition.End} and return the current {@code StepBuilder}. For example,
     * <p/>
     * <pre>
     * endOn("END").exitStatus("new status for end").&lt;other StepBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "end" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.End<StepBuilder>}
     *
     * @see org.jberet.job.model.Transition.End
     * @see DecisionBuilder#endOn(java.lang.String)
     * @see FlowBuilder#endOn(String)
     */
    public Transition.End<StepBuilder> endOn(final String exitStatusCondition) {
        final Transition.End<StepBuilder> end = new Transition.End<StepBuilder>(exitStatusCondition);
        end.enclosingBuilder = this;
        transitions.add(end);
        return end;
    }

    /**
     * Sets {@code fail} transition condition for the step. This method does NOT return the current {@code StepBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Fail}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Fail#exitStatus(String...)} will end the operation on
     * {@code Transition.Fail} and return the current {@code StepBuilder}. For example,
     * <p/>
     * <pre>
     * failOn("FAIL").exitStatus("new status for fail").&lt;other StepBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "fail" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Fail<StepBuilder>}
     *
     * @see org.jberet.job.model.Transition.Fail
     * @see DecisionBuilder#failOn(java.lang.String)
     * @see FlowBuilder#failOn(String)
     */
    public Transition.Fail<StepBuilder> failOn(final String exitStatusCondition) {
        final Transition.Fail<StepBuilder> fail = new Transition.Fail<StepBuilder>(exitStatusCondition);
        fail.enclosingBuilder = this;
        transitions.add(fail);
        return fail;
    }

    /**
     * Sets {@code stop} transition condition for the step. This method does NOT return the current {@code StepBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Stop}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Stop#exitStatus(String...)} will end the operation on
     * {@code Transition.Stop} and return the current {@code StepBuilder}. For example,
     * <p/>
     * <pre>
     * stopOn("STOP").restartFrom("step1").exitStatus().&lt;other StepBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "stop" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Stop<StepBuilder>}
     *
     * @see org.jberet.job.model.Transition.Stop
     * @see DecisionBuilder#stopOn(java.lang.String)
     * @see FlowBuilder#stopOn(String)
     */
    public Transition.Stop<StepBuilder> stopOn(final String exitStatusCondition) {
        final Transition.Stop<StepBuilder> stop = new Transition.Stop<StepBuilder>(exitStatusCondition, null);
        stop.enclosingBuilder = this;
        transitions.add(stop);
        return stop;
    }

    /**
     * Sets {@code next} transition condition for the step. This method does NOT return the current {@code StepBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Next}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Next#to(String)} will end the operation on
     * {@code Transition.Next} and return the current {@code StepBuilder}. For example,
     * <p/>
     * <pre>
     * nextOn("*").to("step2").&lt;other StepBuilder methods&gt;
     * </pre>
     * @param exitStatusCondition exit status condition to trigger "next" action(may contain wildcard ? and *)
     * @return an instance of {@code Transition.Next<StepBuilder>}
     *
     * @see org.jberet.job.model.Transition.Next
     * @see DecisionBuilder#nextOn(java.lang.String)
     * @see FlowBuilder#nextOn(String)
     */
    public Transition.Next<StepBuilder> nextOn(final String exitStatusCondition) {
        final Transition.Next<StepBuilder> nx = new Transition.Next<StepBuilder>(exitStatusCondition);
        nx.enclosingBuilder = this;
        transitions.add(nx);
        return nx;
    }

    /**
     * Sets the partition mapper for a partitioned step. The mapper may carry 0 or more properties. Each mapper
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * partitionMapper("mapper1");
     * partitionMapper("partitionMapper2", new String[]{"key1", "value1"});
     * partitionMapper("partitionMapper3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * partitionMapper("partitionMapper4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param partitionMapperRef partition mapper name
     * @param pairsOfKeyValue optional partition mapper properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionMapper(final String partitionMapperRef, final String[]... pairsOfKeyValue) {
        partition.setMapper(JobBuilder.createRefArtifactWithProperties(partitionMapperRef, null, pairsOfKeyValue));
        return this;
    }

    /**
     * Sets the partition mapper for a partitioned step, with partition mapper properties.
     *
     * @param partitionMapperRef partition mapper name
     * @param props partition mapper properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionMapper(final String partitionMapperRef, final java.util.Properties props) {
        partition.setMapper(JobBuilder.createRefArtifactWithProperties(partitionMapperRef, props));
        return this;
    }

    /**
     * Sets the partition collector for a partitioned step. The collector may carry 0 or more properties. Each collector
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * partitionCollector("partitionCollector1");
     * partitionCollector("partitionCollector2", new String[]{"key1", "value1"});
     * partitionCollector("partitionCollector3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * partitionCollector("partitionCollector4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param partitionCollectorRef partition collector name
     * @param pairsOfKeyValue optional partition collector properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionCollector(final String partitionCollectorRef, final String[]... pairsOfKeyValue) {
        partition.setCollector(JobBuilder.createRefArtifactWithProperties(partitionCollectorRef, null, pairsOfKeyValue));
        return this;
    }

    /**
     * Sets the partition collector for a partitioned step, with partition collector properties.
     *
     * @param partitionCollectorRef partition collector name
     * @param props partition collector properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionCollector(final String partitionCollectorRef, final java.util.Properties props) {
        partition.setCollector(JobBuilder.createRefArtifactWithProperties(partitionCollectorRef, props));
        return this;
    }

    /**
     * Sets the partition analyzer for a partitioned step. The analyzer may carry 0 or more properties. Each analyzer
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * partitionAnalyzer("partitionAnalyzer1");
     * partitionAnalyzer("partitionAnalyzer2", new String[]{"key1", "value1"});
     * partitionAnalyzer("partitionAnalyzer3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * partitionAnalyzer("partitionAnalyzer4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param partitionAnalyzerRef partition analyzer name
     * @param pairsOfKeyValue optional partition analyzer properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionAnalyzer(final String partitionAnalyzerRef, final String[]... pairsOfKeyValue) {
        partition.setAnalyzer(JobBuilder.createRefArtifactWithProperties(partitionAnalyzerRef, null, pairsOfKeyValue));
        return this;
    }

    /**
     * Sets the partition analyzer for a partitioned step, with partition analyzer properties.
     *
     * @param partitionAnalyzerRef partition analyzer name
     * @param props partition analyzer properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionAnalyzer(final String partitionAnalyzerRef, final java.util.Properties props) {
        partition.setAnalyzer(JobBuilder.createRefArtifactWithProperties(partitionAnalyzerRef, props));
        return this;
    }

    /**
     * Sets the partition reducer for a partitioned step. The reducer may carry 0 or more properties. Each reducer
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * partitionReducer("partitionReducer1");
     * partitionReducer("partitionReducer2", new String[]{"key1", "value1"});
     * partitionReducer("partitionAnalyzer3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * partitionAnalyzer("partitionAnalyzer4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param partitionReducerRef partition reducer name
     * @param pairsOfKeyValue optional partition reducer properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionReducer(final String partitionReducerRef, final String[]... pairsOfKeyValue) {
        partition.setReducer(JobBuilder.createRefArtifactWithProperties(partitionReducerRef, null, pairsOfKeyValue));
        return this;
    }

    /**
     * Sets the partition reducer for a partitioned step, with partition reducer properties.
     *
     * @param partitionReducerRef partition reducer name
     * @param props partition reducer properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder partitionReducer(final String partitionReducerRef, final java.util.Properties props) {
        partition.setReducer(JobBuilder.createRefArtifactWithProperties(partitionReducerRef, props));
        return this;
    }

    /**
     * Sets the partition plan for a partitioned step. Partition properties in {@code listOfPartitionProps} should be
     * ordered by partition index, i.e., {@code listOfPartitionProps.get(0)} is the partition properties for partition
     * with index 0, {@code listOfPartitionProps.get(1)} is the partition properties for partition
     * with index 1, etc.
     *
     * @param partitionCount number of partitions
     * @param threadCount max number of threads for partition execution
     * @param listOfPartitionProps list of partition properties
     * @return this {@code StepBuilder}
     *
     * @see #partitionPlan(int, List)
     * @see #partitionPlan(int, int...)
     */
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

    /**
     * Sets the partition plan for a partitioned step. Partition properties in {@code listOfPartitionProps} should be
     * ordered by partition index, i.e., {@code listOfPartitionProps.get(0)} is the partition properties for partition
     * with index 0, {@code listOfPartitionProps.get(1)} is the partition properties for partition
     * with index 1, etc.
     *
     * @param partitionCount number of partitions
     * @param listOfPartitionProps list of partition properties
     * @return this {@code StepBuilder}
     *
     * @see #partitionPlan(int, int, List)
     * @see #partitionPlan(int, int...)
     */
    public StepBuilder partitionPlan(final int partitionCount, final List<java.util.Properties> listOfPartitionProps) {
        return partitionPlan(partitionCount, partitionCount, listOfPartitionProps);
    }

    /**
     * Sets the partition plan for a partitioned step, with optional {@code threadCount} and without partition properties.
     *
     * @param partitionCount number of partitions
     * @param threadCount max number of threads for partition execution
     * @return this {@code StepBuilder}
     *
     * @see #partitionPlan(int, int, List)
     * @see #partitionPlan(int, List)
     */
    public StepBuilder partitionPlan(final int partitionCount, final int... threadCount) {
        return partitionPlan(partitionCount, threadCount.length == 0 ? partitionCount : threadCount[0], null);
    }

    /**
     * Sets item reader for a chunk-type step. The reader may carry 0 or more properties. Each reader
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * reader("reader1");
     * reader("reader2", new String[]{"key1", "value1"});
     * reader("reader3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * reader("reader4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param readerRef item reader name
     * @param pairsOfKeyValue optional reader properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder reader(final String readerRef, final String[]... pairsOfKeyValue) {
        chunk.reader = JobBuilder.createRefArtifactWithProperties(readerRef, null, pairsOfKeyValue);
        return this;
    }

    /**
     * Sets item reader for a chunk-type step, with item reader properties.
     *
     * @param readerRef item reader name
     * @param props item reader properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder reader(final String readerRef, final java.util.Properties props) {
        chunk.reader = JobBuilder.createRefArtifactWithProperties(readerRef, props);
        return this;
    }

    /**
     * Sets item writer for a chunk-type step. The writer may carry 0 or more properties. Each writer
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * writer("writer1");
     * writer("writer2", new String[]{"key1", "value1"});
     * writer("writer3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * writer("writer4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param writerRef item writer name
     * @param pairsOfKeyValue optional writer properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder writer(final String writerRef, final String[]... pairsOfKeyValue) {
        chunk.writer = JobBuilder.createRefArtifactWithProperties(writerRef, null, pairsOfKeyValue);
        return this;
    }

    /**
     * Sets item writer for a chunk-type step, with item writer properties.
     * @param writerRef item writer name
     * @param props item writer properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder writer(final String writerRef, final java.util.Properties props) {
        chunk.writer = JobBuilder.createRefArtifactWithProperties(writerRef, props);
        return this;
    }

    /**
     * Sets item processor for a chunk-type step. The processor may carry 0 or more properties. Each processor
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * processor("processor1");
     * processor("processor2", new String[]{"key1", "value1"});
     * processor("processor3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * processor("processor4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param processorRef item processor name
     * @param pairsOfKeyValue optional processor properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder processor(final String processorRef, final String[]... pairsOfKeyValue) {
        chunk.processor = JobBuilder.createRefArtifactWithProperties(processorRef, null, pairsOfKeyValue);
        return this;
    }

    /**
     * Sets item processor for a chunk-type step, with item processor properties.
     *
     * @param processorRef item processor name
     * @param props item processor properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder processor(final String processorRef, final java.util.Properties props) {
        chunk.processor = JobBuilder.createRefArtifactWithProperties(processorRef, props);
        return this;
    }

    /**
     * Sets checkpoint algorithm for a chunk-type step. The checkpoint algorithm may carry 0 or more properties.
     * Each property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * checkpointAlgorithm("checkpointAlgorithm1");
     * checkpointAlgorithm("checkpointAlgorithm2", new String[]{"key1", "value1"});
     * checkpointAlgorithm("checkpointAlgorithm3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * checkpointAlgorithm("checkpointAlgorithm4", new String[]{"key1", "#{jobParameters['PropVal1']}"}
     * </pre>
     *
     * @param checkpointAlgorithmRef checkpoint algorithm artifact name
     * @param pairsOfKeyValue optional properties in the form of a series of 2-element string arrays
     * @return this {@code StepBuilder}
     */
    public StepBuilder checkpointAlgorithm(final String checkpointAlgorithmRef, final String[]... pairsOfKeyValue) {
        chunk.checkpointAlgorithm = JobBuilder.createRefArtifactWithProperties(checkpointAlgorithmRef, null, pairsOfKeyValue);
        return this;
    }

    /**
     * Sets checkpoint algorithm for a chunk-type step, with checkpoint algorithm properties.
     *
     * @param checkpointAlgorithmRef checkpoint algorithm artifact name
     * @param props checkpoint algorithm properties, null means no properties
     * @return this {@code StepBuilder}
     */
    public StepBuilder checkpointAlgorithm(final String checkpointAlgorithmRef, final java.util.Properties props) {
        chunk.checkpointAlgorithm = JobBuilder.createRefArtifactWithProperties(checkpointAlgorithmRef, props);
        return this;
    }

    /**
     * Sets checkpoint policy for a chunk-type step. Valid values are "item" (the default) and "custom".
     *
     * @param checkpointPolicy checkpoint policy value, either "item" or "custom"
     * @return this {@code StepBuilder}
     */
    public StepBuilder checkpointPolicy(final String checkpointPolicy) {
        chunk.checkpointPolicy = checkpointPolicy;
        return this;
    }

    /**
     * Sets {@code item-count} attribue value for a chunk-type step.
     *
     * @param itemCount {@code item-count} value
     * @return this {@code StepBuilder}
     */
    public StepBuilder itemCount(final int itemCount) {
        chunk.itemCount = String.valueOf(itemCount);
        return this;
    }

    /**
     * Sets {@code time-limit} attribute value for a chunk-type step.
     *
     * @param timeLimit {@code time-limit} value
     * @param timeUnit time unit such as {@code TimeUnit.SECOND}
     * @return this {@code StepBuilder}
     */
    public StepBuilder timeLimit(final int timeLimit, final TimeUnit... timeUnit) {
        if (timeUnit.length == 0) {
            chunk.timeLimit = String.valueOf(timeLimit);
        } else {
            chunk.timeLimit = String.valueOf(TimeUnit.SECONDS.convert(timeLimit, timeUnit[0]));
        }
        return this;
    }

    /**
     * Sets {@code skip-limit} attribute value for a chunk-type step.
     *
     * @param skipLimit {@code skip-limit} value
     * @return this {@code StepBuilder}
     */
    public StepBuilder skipLimit(final int skipLimit) {
        chunk.skipLimit = String.valueOf(skipLimit);
        return this;
    }

    /**
     * Sets {@code retry-limit} attribute value for a chunk-type step.
     *
     * @param retryLimit {@code retry-limit} value
     * @return this {@code StepBuilder}
     */
    public StepBuilder retryLimit(final int retryLimit) {
        chunk.retryLimit = String.valueOf(retryLimit);
        return this;
    }


    /**
     * Adds exception classes to the chunk-type step to include as skippable.
     *
     * @param exceptionClasses exception classes to include as skippable
     * @return this {@code StepBuilder}
     *
     * @see #skippableExceptionsExclude(Class[])
     */
    public StepBuilder skippableExceptionsInclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.skippableExceptionClasses == null) {
            chunk.skippableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.skippableExceptionClasses.include.add(cl.getName());
        }
        return this;
    }

    /**
     * Adds exception classes to the chunk-type step to exclude from skippable exceptions.
     *
     * @param exceptionClasses exception classes to exclude from skippable exceptions
     * @return this {@code StepBuilder}
     *
     * @see #skippableExceptionsInclude(Class[])
     */
    public StepBuilder skippableExceptionsExclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.skippableExceptionClasses == null) {
            chunk.skippableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.skippableExceptionClasses.exclude.add(cl.getName());
        }
        return this;
    }

    /**
     * Adds exception classes to the chunk-type step to include as retryable.
     *
     * @param exceptionClasses exception classes to include as retryable
     * @return this {@code StepBuilder}
     *
     * @see #retryableExceptionsExclude(Class[])
     */
    public StepBuilder retryableExceptionsInclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.retryableExceptionClasses == null) {
            chunk.retryableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.retryableExceptionClasses.include.add(cl.getName());
        }
        return this;
    }

    /**
     * Adds exception classes to the chunk-type step to exclude from retryable exceptions.
     *
     * @param exceptionClasses exception classes to exclude from retryable exceptions
     * @return this {@code StepBuilder}
     *
     * @see #retryableExceptionsInclude(Class[])
     */
    public StepBuilder retryableExceptionsExclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.retryableExceptionClasses == null) {
            chunk.retryableExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.retryableExceptionClasses.exclude.add(cl.getName());
        }
        return this;
    }

    /**
     * Adds exception classes to the chunk-type step to include as no-rollback exceptions.
     *
     * @param exceptionClasses exception classes to include as no-rollback exceptions
     * @return this {@code StepBuilder}
     *
     * @see #noRollbackExceptionsExclude(Class[])
     */
    public StepBuilder noRollbackExceptionsInclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.noRollbackExceptionClasses == null) {
            chunk.noRollbackExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.noRollbackExceptionClasses.include.add(cl.getName());
        }
        return this;
    }

    /**
     * Adds exception classes to the chunk-type step to exclude from no-rollback exceptions.
     *
     * @param exceptionClasses exception classes to exclude from no-rollback exceptions
     * @return this {@code StepBuilder}
     *
     * @see #noRollbackExceptionsInclude(Class[])
     */
    public StepBuilder noRollbackExceptionsExclude(final Class<? extends Exception>... exceptionClasses) {
        if (chunk.noRollbackExceptionClasses == null) {
            chunk.noRollbackExceptionClasses = new ExceptionClassFilter();
        }
        for (final Class<? extends Exception> cl : exceptionClasses) {
            chunk.noRollbackExceptionClasses.exclude.add(cl.getName());
        }
        return this;
    }


    /**
     * Builds the step. After this method, this {@code StepBuilder} should not be reused to build another step.
     *
     * @return a step built by this {@code StepBuilder}
     */
    public Step build() {
        final Step step = new Step(id);
        step.startLimit = String.valueOf(startLimit);
        step.allowStartIfComplete = String.valueOf(allowStartIfComplete);
        step.next = next;
        step.setListeners(listeners);

        if (nameValues.size() > 0) {
            step.setProperties(nameValuesToProperties());
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
