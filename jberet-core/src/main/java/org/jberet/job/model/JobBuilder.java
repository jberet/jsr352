/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.batch.operations.BatchRuntimeException;

import org.jberet._private.BatchMessages;

/**
 * Builder class for building a single {@linkplain Job job}. After the job is built, the same {@code JobBuilder} instance
 * should not be reused to build another job.
 * <p/>
 * Unlike XML JSL, jobs built programmatically with this builder and other related builder classes do not support
 * JSL inheritance, nor artifacts written in scripting languages.
 * <p/>
 * This class does not support multi-threaded access or modification. Usage example,
 * <p/>
 * <pre>
 *     Job job = new JobBuilder(jobName)
 *              .restartable(false)
 *              .property("jobk1", "J")
 *              .property("jobk2", "J")
 *              .listener("jobListener1", new String[]{"jobListenerk1", "#{jobParameters['jobListenerPropVal']}"},
 *                      new String[]{"jobListenerk2", "#{jobParameters['jobListenerPropVal']}"})
 *
 *              .step(new StepBuilder(stepName)
 *                      .properties(new String[]{"stepk1", "S"}, new String[]{"stepk2", "S"})
 *                      .batchlet(batchlet1Name, new String[]{"batchletk1", "B"}, new String[]{"batchletk2", "B"})
 *                      .listener("stepListener1", stepListenerProps)
 *                      .stopOn("STOP").restartFrom(stepName).exitStatus()
 *                      .endOn("END").exitStatus("new status for end")
 *                      .failOn("FAIL").exitStatus()
 *                      .nextOn("*").to(step2Name)
 *                      .build())
 *
 *              .step(new StepBuilder(step2Name)
 *                      .batchlet(batchlet1Name)
 *                      .build())
 *
 *              .build();
 * </pre>
 *
 * @see StepBuilder
 * @see FlowBuilder
 * @see SplitBuilder
 * @see DecisionBuilder
 * @since 1.2.0
 */
public final class JobBuilder extends AbstractPropertiesBuilder<JobBuilder> {
    private final String id;
    private String restartable;
    private Listeners listeners;
    private final List<JobElement> jobElements = new ArrayList<JobElement>();

    /**
     * stores ids for all job elements: job, step, flow, split and decision. These ids should be checked for uniqueness.
     */
    final Set<String> ids = new HashSet<String>();

    /**
     * Constructs a {@code JobBuilder} for building the job with the specified {@code id}.
     *
     * @param id the job id, corresponding to the id attribute of jsl:Job element in XML
     */
    public JobBuilder(final String id) {
        this.id = id;
    }

    /**
     * Sets the {@code restartable} attribute value on the job. This method may be invoked with 0 or 1 boolean parameter.
     * {@code restartable()} is equivalent to {@code restartable(true)}.
     *
     * @param b optional restartable value (true or false)
     * @return this {@code JobBuilder}
     */
    public JobBuilder restartable(final boolean... b) {
        if (b.length == 0) {
            this.restartable = String.valueOf(true);
        } else {
            this.restartable = String.valueOf(b[0]);
        }
        return this;
    }

    /**
     * Adds a job listener to the job. The listener may be added with 0 or more listener properties. Each listener
     * property is represented by a 2-element string array, whose 1st element is the property key, and 2nd element is
     * the property value. For example,
     * <p/>
     * <pre>
     * listener("listener1");
     * listener1("listener2", new String[]{"key1", "value1"});
     * listener1("listener3", new String[]{"key1", "value1"}, new String[]{"key2", "value2"});
     * listener1("listener4", new String[]{"jobListenerk1", "#{jobParameters['jobListenerPropVal']}"}
     * </pre>
     *
     * @param listenerRef job listener name
     * @param pairsOfKeyValue optional listener properties in the form of a series of 2-element string arrays
     * @return this {@code JobBuilder}
     */
    public JobBuilder listener(final String listenerRef, final String[]... pairsOfKeyValue) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(createRefArtifactWithProperties(listenerRef, null, pairsOfKeyValue));
        return this;
    }

    /**
     * Adds a job listener to the job, with listener properties.
     *
     * @param listenerRef job listener name
     * @param props job listener properties, null means no properties
     * @return this {@code JobBuilder}
     */
    public JobBuilder listener(final String listenerRef, final java.util.Properties props) {
        if (listeners == null) {
            listeners = new Listeners();
        }
        listeners.getListeners().add(createRefArtifactWithProperties(listenerRef, props));
        return this;
    }

    /**
     * Adds a {@linkplain Step step} to the job. The step is typically built with {@link StepBuilder}.
     *
     * @param step a pre-built step
     * @return this {@code JobBuilder}
     */
    public JobBuilder step(final Step step) {
        jobElements.add(step);
        return this;
    }

    /**
     * Adds a {@linkplain Decision decision} to the job. The decision is typically built with {@link DecisionBuilder}.
     *
     * @param decision a pre-built decision
     * @return this {@code JobBuilder}
     */
    public JobBuilder decision(final Decision decision) {
        jobElements.add(decision);
        return this;
    }

    /**
     * Adds a {@linkplain Flow flow} to the job. The flow is typically built with {@link FlowBuilder}.
     *
     * @param flow a pre-built flow
     * @return this {@code JobBuilder}
     */
    public JobBuilder flow(final Flow flow) {
        jobElements.add(flow);
        return this;
    }

    /**
     * Adds a {@linkplain Split split} to the job. The split is typically built with {@link SplitBuilder}.
     *
     * @param split a pre-built split
     * @return this {@code JobBuilder}
     */
    public JobBuilder split(final Split split) {
        jobElements.add(split);
        return this;
    }

    /**
     * Builds the job. This method also verifies the uniqueness of all id values within the job.
     * After this method, this {@code JobBuilder} should not be used to build another job.
     *
     * @return a job built by this {@code JobBuilder}
     */
    public Job build() {
        final Job job = new Job(id);

        if (restartable != null) {
            job.setRestartable(restartable);
        }
        if (nameValues.size() > 0) {
            job.setProperties(nameValuesToProperties());
        }

        job.setListeners(listeners);

        ids.add(id);
        for (final JobElement jobElement : jobElements) {
            assertUniqueId(jobElement);
            job.addJobElement(jobElement);
        }

        jobElements.clear();
        ids.clear();
        return job;
    }

    /**
     * Creates {@link RefArtifact} with optional properties. If {@code props} is not null, it is taken as the artifact
     * properties, and {@code propKeysValues} is ignored. If {@code props} is null, {@code propKeysValues} is taken as
     * the artifact properties.
     *
     * @param ref batch artifact name
     * @param props artifact properties, may be null
     * @param propKeysValues optional artifact properties as a series of 2-element string arrays
     *
     * @return created {@code RefArtifact}
     */
    static RefArtifact createRefArtifactWithProperties(final String ref,
                                                       final java.util.Properties props,
                                                       final String[]... propKeysValues) {
        final RefArtifact refArtifact = new RefArtifact(ref);
        final Properties properties = new Properties();
        if (props != null) {
            for (final String k : props.stringPropertyNames()) {
                properties.add(k, props.getProperty(k));
            }
        } else if (propKeysValues.length > 0) {
            for (final String[] pair : propKeysValues) {
                properties.add(pair[0], pair.length > 1 ? pair[1] : null);
            }
        }
        refArtifact.setProperties(properties);
        return refArtifact;
    }

    /**
     * Asserts all id values within a job are unique.
     *
     * @param jobElement a job element: step, decision, flow, or split
     * @throws BatchRuntimeException in case of duplicate id values within a job
     */
    private void assertUniqueId(final JobElement jobElement) throws BatchRuntimeException {
        final String jobElementId = jobElement.getId();
        if (!ids.add(jobElementId)) {
            throw BatchMessages.MESSAGES.idAlreadyExists(jobElement.getClass().getSimpleName(), jobElementId);
        }
        if (jobElement instanceof Split) {
            for (final Flow f : ((Split) jobElement).flows) {
                assertUniqueId(f);
            }
        } else if (jobElement instanceof Flow) {
            for (final JobElement e : ((Flow) jobElement).jobElements) {
                assertUniqueId(e);
            }
        }
    }
}
