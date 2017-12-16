/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
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

import org.jberet.testapps.common.StepNameHolder;

/**
 * This class does not declare any CDI scope at the type level.
 * When injecting into a field, the field should declare one of the
 * JBeret CDI scopes:
 * <ul>
 *     <li>{@link org.jberet.cdi.JobScoped}
 *     <li>{@link org.jberet.cdi.StepScoped}
 *     <li>{@link org.jberet.cdi.PartitionScoped}
 * </ul>
 *
 * @since 1.3.0.Final
 */
public class FooFieldTarget extends StepNameHolder {
}
