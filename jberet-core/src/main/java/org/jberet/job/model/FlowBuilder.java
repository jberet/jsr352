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

public final class FlowBuilder {
    private final String id;
    private String next;
    private final List<Transition> transitions = new ArrayList<Transition>();
    private final List<JobElement> jobElements = new ArrayList<JobElement>();

    public FlowBuilder(final String id) {
        this.id = id;
    }

    public FlowBuilder next(final String next) {
        this.next = next;
        return this;
    }

    public Transition.End<FlowBuilder> endOn(final String exitStatus) {
        final Transition.End<FlowBuilder> end = new Transition.End<FlowBuilder>(exitStatus);
        end.enclosingBuilder = this;
        transitions.add(end);
        return end;
    }

    public Transition.Fail<FlowBuilder> failOn(final String exitStatus) {
        final Transition.Fail<FlowBuilder> fail = new Transition.Fail<FlowBuilder>(exitStatus);
        fail.enclosingBuilder = this;
        transitions.add(fail);
        return fail;
    }

    public Transition.Stop<FlowBuilder> stopOn(final String exitStatus) {
        final Transition.Stop<FlowBuilder> stop = new Transition.Stop<FlowBuilder>(exitStatus, null);
        stop.enclosingBuilder = this;
        transitions.add(stop);
        return stop;
    }

    public Transition.Next<FlowBuilder> nextOn(final String exitStatus) {
        final Transition.Next<FlowBuilder> nx = new Transition.Next<FlowBuilder>(exitStatus);
        nx.enclosingBuilder = this;
        transitions.add(nx);
        return nx;
    }


    public FlowBuilder decision(final Decision decision) {
        jobElements.add(decision);
        return this;
    }

    public FlowBuilder flow(final Flow flow) {
        jobElements.add(flow);
        return this;
    }

    public FlowBuilder split(final Split split) {
        jobElements.add(split);
        return this;
    }

    public FlowBuilder step(final Step step) {
        jobElements.add(step);
        return this;
    }


    public Flow build() {
        final Flow flow = new Flow(id);
        flow.next = next;
        flow.getTransitionElements().addAll(transitions);
        flow.jobElements = jobElements;
        return flow;
    }
}
