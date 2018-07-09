/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.commons;

/**
 * This class does not declare any CDI scope at the type level.
 * When produced by a producer method, the field should declare one of the
 * JBeret CDI scopes:
 * <ul>
 *     <li>{@link org.jberet.cdi.JobScoped}
 *     <li>{@link org.jberet.cdi.StepScoped}
 *     <li>{@link org.jberet.cdi.PartitionScoped}
 * </ul>
 *
 * @since 1.3.0.Final
 */
public class FooMethodTarget extends StepNameHolder {
}
