/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.cdiscopes.stepscoped;

import javax.inject.Named;

import org.jberet.cdi.StepScoped;
import org.jberet.testapps.cdiscopes.commons.StepNameHolder;

/**
 * This bean class is annotated with {@link StepScoped} at the type level,
 * so any injected instance will have step scope as defined in {@code StepScope}.
 */
@Named
@StepScoped
public class Foo extends StepNameHolder {
}
