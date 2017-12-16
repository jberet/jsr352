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

package org.jberet.testapps.cdiscopes.jobscoped;

import javax.inject.Named;

import org.jberet.cdi.JobScoped;
import org.jberet.testapps.common.StepNameHolder;

/**
 * This bean class is annotated with {@link JobScoped} at the type level,
 * so any injected instance will have job scope as defined in {@code JobScope}.
 */
@Named
@JobScoped
public class Foo extends StepNameHolder {
}
