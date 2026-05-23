/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.util.List;

class SecurityActions {
    static List<JobElement> cloneJobElements(List<JobElement> jobElements) {
        return JobFactory.cloneJobElements(jobElements);
    }
}
