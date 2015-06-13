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

public final class DecisionBuilder extends AbstractPropertiesBuilder<DecisionBuilder> {
    private final String id;
    private final String ref;
    private final List<Transition> transitions = new ArrayList<Transition>();

    public DecisionBuilder(final String id, final String deciderRef) {
        this.id = id;
        this.ref = deciderRef;
    }

    public Transition.End<DecisionBuilder> endOn(final String exitStatus) {
        final Transition.End<DecisionBuilder> end = new Transition.End<DecisionBuilder>(exitStatus);
        end.enclosingBuilder = this;
        transitions.add(end);
        return end;
    }

    public Transition.Fail<DecisionBuilder> failOn(final String exitStatus) {
        final Transition.Fail<DecisionBuilder> fail = new Transition.Fail<DecisionBuilder>(exitStatus);
        fail.enclosingBuilder = this;
        transitions.add(fail);
        return fail;
    }

    public Transition.Stop<DecisionBuilder> stopOn(final String exitStatus) {
        final Transition.Stop<DecisionBuilder> stop = new Transition.Stop<DecisionBuilder>(exitStatus, null);
        stop.enclosingBuilder = this;
        transitions.add(stop);
        return stop;
    }

    public Transition.Next<DecisionBuilder> nextOn(final String exitStatus) {
        final Transition.Next<DecisionBuilder> nx = new Transition.Next<DecisionBuilder>(exitStatus);
        nx.enclosingBuilder = this;
        transitions.add(nx);
        return nx;
    }

    public Decision build() {
        final Decision decision = new Decision(id, ref);
        decision.getTransitionElements().addAll(this.transitions);

        if (nameValues.size() > 0) {
            decision.setProperties(nameValuesToProperties(nameValues));
        }
        return decision;
    }
}
