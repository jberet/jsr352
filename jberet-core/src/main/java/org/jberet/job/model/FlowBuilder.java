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

/**
 * Builder class for building a single {@link Flow}. After the flow is built, the same {@code FlowBuilder}
 * instance should not be reused to build another flow.
 * <p/>
 * This class does not support multi-threaded access or modification. Usage example:
 * <p/>
 * <pre>
 *      Flow flow = new FlowBuilder(flowName)
 *              .step(new StepBuilder(stepName).batchlet(batchlet1Name)
 *                      .next(step2Name)
 *                      .build())
 *              .step(new StepBuilder(step2Name).batchlet(batchlet1Name)
 *                      .build())
 *              .build())
 * </pre>
 *
 * @see JobBuilder
 * @see DecisionBuilder
 * @see SplitBuilder
 * @see StepBuilder
 *
 * @since 1.2.0
 */
public final class FlowBuilder {
    private final String id;
    private String next;
    private final List<Transition> transitions = new ArrayList<Transition>();
    private final List<JobElement> jobElements = new ArrayList<JobElement>();

    /**
     * Constructs the {@code FlowBuilder} instance for building the {@linkplain Flow flow} with the specified {@code id}.
     * @param id flow id
     */
    public FlowBuilder(final String id) {
        this.id = id;
    }

    /**
     * Sets the {@code next} attribute value for the flow.
     *
     * @param next id of the next job element after the flow
     * @return this {@code FlowBuilder}
     */
    public FlowBuilder next(final String next) {
        this.next = next;
        return this;
    }

    /**
     * Sets {@code end} transition condition for the flow. This method does NOT return the current {@code FlowBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.End}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.End#exitStatus(String...)} will end the operation on
     * {@code Transition.End} and return the current {@code FlowBuilder}. For example,
     * <p/>
     * <pre>
     * endOn("END").exitStatus("new status for end").&lt;other FlowBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "end" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.End<FlowBuilder>}
     *
     * @see org.jberet.job.model.Transition.End
     * @see StepBuilder#endOn(java.lang.String)
     * @see DecisionBuilder#endOn(String)
     */
    public Transition.End<FlowBuilder> endOn(final String exitStatusCondition) {
        final Transition.End<FlowBuilder> end = new Transition.End<FlowBuilder>(exitStatusCondition);
        end.enclosingBuilder = this;
        transitions.add(end);
        return end;
    }

    /**
     * Sets {@code fail} transition condition for the flow. This method does NOT return the current {@code FlowBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Fail}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Fail#exitStatus(String...)} will end the operation on
     * {@code Transition.Fail} and return the current {@code FlowBuilder}. For example,
     * <p/>
     * <pre>
     * failOn("FAIL").exitStatus("new status for fail").&lt;other FlowBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "fail" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Fail<FlowBuilder>}
     *
     * @see org.jberet.job.model.Transition.Fail
     * @see StepBuilder#failOn(java.lang.String)
     * @see DecisionBuilder#failOn(String)
     */
    public Transition.Fail<FlowBuilder> failOn(final String exitStatusCondition) {
        final Transition.Fail<FlowBuilder> fail = new Transition.Fail<FlowBuilder>(exitStatusCondition);
        fail.enclosingBuilder = this;
        transitions.add(fail);
        return fail;
    }

    /**
     * Sets {@code stop} transition condition for the flow. This method does NOT return the current {@code FlowBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Stop}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Stop#exitStatus(String...)} will end the operation on
     * {@code Transition.Stop} and return the current {@code FlowBuilder}. For example,
     * <p/>
     * <pre>
     * stopOn("STOP").restartFrom("step1").exitStatus().&lt;other FlowBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "stop" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Stop<FlowBuilder>}
     *
     * @see org.jberet.job.model.Transition.Stop
     * @see StepBuilder#stopOn(java.lang.String)
     * @see DecisionBuilder#stopOn(String)
     */
    public Transition.Stop<FlowBuilder> stopOn(final String exitStatusCondition) {
        final Transition.Stop<FlowBuilder> stop = new Transition.Stop<FlowBuilder>(exitStatusCondition, null);
        stop.enclosingBuilder = this;
        transitions.add(stop);
        return stop;
    }

    /**
     * Sets {@code next} transition condition for the flow. This method does NOT return the current {@code FlowBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Next}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Next#to(String)} will end the operation on
     * {@code Transition.Next} and return the current {@code FlowBuilder}. For example,
     * <p/>
     * <pre>
     * nextOn("*").to("step2").&lt;other FlowBuilder methods&gt;
     * </pre>
     * @param exitStatusCondition exit status condition to trigger "next" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Next<FlowBuilder>}
     *
     * @see org.jberet.job.model.Transition.Next
     * @see StepBuilder#nextOn(java.lang.String)
     * @see DecisionBuilder#nextOn(String)
     */
    public Transition.Next<FlowBuilder> nextOn(final String exitStatusCondition) {
        final Transition.Next<FlowBuilder> nx = new Transition.Next<FlowBuilder>(exitStatusCondition);
        nx.enclosingBuilder = this;
        transitions.add(nx);
        return nx;
    }

    /**
     * Adds a {@linkplain Decision decision} to the current flow.
     *
     * @param decision the decision to be added to the current flow
     * @return this {@code FlowBuilder}
     */
    public FlowBuilder decision(final Decision decision) {
        jobElements.add(decision);
        return this;
    }

    /**
     * Adds a {@linkplain Flow flow} to the flow.
     *
     * @param flow the flow to be added to the current flow
     * @return this {@code FlowBuilder}
     */
    public FlowBuilder flow(final Flow flow) {
        jobElements.add(flow);
        return this;
    }

    /**
     * Adds a {@linkplain Split split} to the current flow.
     *
     * @param split the split to be added to the current flow
     * @return this {@code FlowBuilder}
     */
    public FlowBuilder split(final Split split) {
        jobElements.add(split);
        return this;
    }

    /**
     * Adds a {@linkplain Step step} to the current flow.
     *
     * @param step the step to be added to the current flow
     * @return this {@code FlowBuilder}
     */
    public FlowBuilder step(final Step step) {
        jobElements.add(step);
        return this;
    }

    /**
     * Builds the {@linkplain Flow flow}.
     *
     * @return the flow built with this {@code FlowBuilder}
     */
    public Flow build() {
        final Flow flow = new Flow(id);
        flow.next = next;
        flow.getTransitionElements().addAll(transitions);
        flow.jobElements = jobElements;
        return flow;
    }
}
