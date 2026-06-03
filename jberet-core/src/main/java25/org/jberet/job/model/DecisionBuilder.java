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
import java.util.List;

/**
 * Builder class for building a single {@link Decision}. After the decision is built, the same {@code DecisionBuilder}
 * instance should not be reused to build another decision.
 * <p/>
 * This class does not support multi-threaded access or modification. Usage example:
 * <p/>
 * <pre>
 * Decision decision = new DecisionBuilder("decision1", "decider1")
 *      .failOn("FAIL").exitStatus()
 *      .stopOn("STOP").restartFrom(stepName).exitStatus()
 *      .nextOn("NEXT").to(stepName)
 *      .endOn("*").exitStatus(stepName)
 *      .build()
 * </pre>
 *
 * @see JobBuilder
 * @see FlowBuilder
 * @see SplitBuilder
 * @see StepBuilder
 *
 * @since 1.2.0
 */
public final class DecisionBuilder extends AbstractPropertiesBuilder<DecisionBuilder> {
    private final String id;
    private final String ref;
    private final List<Transition> transitions = new ArrayList<Transition>();

    /**
     * Constructs a {@code DecisionBuilder} for building a {@linkplain Decision decision} with the specified {@code id} and
     * decider name ({@code deciderRef}).
     *
     * @param id decision id
     * @param deciderRef decider artifact name
     */
    public DecisionBuilder(final String id, final String deciderRef) {
        this.id = id;
        this.ref = deciderRef;
    }

    /**
     * Sets {@code end} transition condition for the decision. This method does NOT return the current {@code DecisionBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.End}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.End#exitStatus(String...)} will end the operation on
     * {@code Transition.End} and return the current {@code DecisionBuilder}. For example,
     * <p/>
     * <pre>
     * endOn("END").exitStatus("new status for end").&lt;other DecisionBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "end" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.End<DecisionBuilder>}
     *
     * @see org.jberet.job.model.Transition.End
     * @see StepBuilder#endOn(java.lang.String)
     * @see FlowBuilder#endOn(String)
     */
    public Transition.End<DecisionBuilder> endOn(final String exitStatusCondition) {
        final Transition.End<DecisionBuilder> end = new Transition.End<DecisionBuilder>(exitStatusCondition);
        end.enclosingBuilder = this;
        transitions.add(end);
        return end;
    }

    /**
     * Sets {@code fail} transition condition for the decision. This method does NOT return the current {@code DecisionBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Fail}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Fail#exitStatus(String...)} will end the operation on
     * {@code Transition.Fail} and return the current {@code DecisionBuilder}. For example,
     * <p/>
     * <pre>
     * failOn("FAIL").exitStatus("new status for fail").&lt;other DecisionBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "fail" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Fail<DecisionBuilder>}
     *
     * @see org.jberet.job.model.Transition.Fail
     * @see StepBuilder#failOn(java.lang.String)
     * @see FlowBuilder#failOn(String)
     */
    public Transition.Fail<DecisionBuilder> failOn(final String exitStatusCondition) {
        final Transition.Fail<DecisionBuilder> fail = new Transition.Fail<DecisionBuilder>(exitStatusCondition);
        fail.enclosingBuilder = this;
        transitions.add(fail);
        return fail;
    }

    /**
     * Sets {@code stop} transition condition for the decision. This method does NOT return the current {@code DecisionBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Stop}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Stop#exitStatus(String...)} will end the operation on
     * {@code Transition.Stop} and return the current {@code DecisionBuilder}. For example,
     * <p/>
     * <pre>
     * stopOn("STOP").restartFrom("step1").exitStatus().&lt;other DecisionBuilder methods&gt;
     * </pre>
     *
     * @param exitStatusCondition exit status condition to trigger "stop" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Stop<DecisionBuilder>}
     *
     * @see org.jberet.job.model.Transition.Stop
     * @see StepBuilder#stopOn(java.lang.String)
     * @see FlowBuilder#stopOn(String)
     */
    public Transition.Stop<DecisionBuilder> stopOn(final String exitStatusCondition) {
        final Transition.Stop<DecisionBuilder> stop = new Transition.Stop<DecisionBuilder>(exitStatusCondition, null);
        stop.enclosingBuilder = this;
        transitions.add(stop);
        return stop;
    }

    /**
     * Sets {@code next} transition condition for the decision. This method does NOT return the current {@code DecisionBuilder}
     * instance; instead, it returns an instance of {@link org.jberet.job.model.Transition.Next}, which can be further
     * operated on. Invoking {@link org.jberet.job.model.Transition.Next#to(String)} will end the operation on
     * {@code Transition.Next} and return the current {@code DecisionBuilder}. For example,
     * <p/>
     * <pre>
     * nextOn("*").to("step2").&lt;other DecisionBuilder methods&gt;
     * </pre>
     * @param exitStatusCondition exit status condition to trigger "next" action (may contain wildcard ? and *)
     * @return an instance of {@code Transition.Next<DecisionBuilder>}
     *
     * @see org.jberet.job.model.Transition.Next
     * @see StepBuilder#nextOn(java.lang.String)
     * @see FlowBuilder#nextOn(String)
     */
    public Transition.Next<DecisionBuilder> nextOn(final String exitStatusCondition) {
        final Transition.Next<DecisionBuilder> nx = new Transition.Next<DecisionBuilder>(exitStatusCondition);
        nx.enclosingBuilder = this;
        transitions.add(nx);
        return nx;
    }

    /**
     * Builds the decision.
     *
     * @return a decision built by this {@code DecisionBuilder}
     */
    public Decision build() {
        final Decision decision = new Decision(id, ref);
        decision.getTransitionElements().addAll(this.transitions);

        if (nameValues.size() > 0) {
            decision.setProperties(nameValuesToProperties());
        }
        return decision;
    }
}
