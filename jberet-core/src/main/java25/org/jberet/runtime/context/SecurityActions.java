/*
 * Copyright (c) 2014-2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.context;

import org.jberet.job.model.JobFactory;
import org.jberet.job.model.Step;

class SecurityActions {

    static Step cloneStep(final Step step) {
        return JobFactory.cloneStep(step);
    }
}
