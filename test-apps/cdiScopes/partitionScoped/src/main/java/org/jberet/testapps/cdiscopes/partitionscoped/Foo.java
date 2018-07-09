/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.partitionscoped;

import javax.inject.Named;

import org.jberet.cdi.PartitionScoped;
import org.jberet.testapps.cdiscopes.commons.StepNameHolder;

/**
 * This bean class is annotated with {@link PartitionScoped} at the type level,
 * so any injected instance will have partition scope as defined in {@code PartitionScoped}.
 */
@Named
@PartitionScoped
public class Foo extends StepNameHolder {
}
