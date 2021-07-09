/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.jobscoped;

import jakarta.inject.Named;

import org.jberet.cdi.JobScoped;
import org.jberet.testapps.cdiscopes.commons.StepNameHolder;

/**
 * This bean class is annotated with {@link JobScoped} at the type level,
 * so any injected instance will have job scope as defined in {@code JobScope}.
 */
@Named
@JobScoped
public class Foo extends StepNameHolder {
}
