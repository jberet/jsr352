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
 
package org.jberet.testapps.javajsl;

import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.inject.Named;

@Named
public class Decider2 implements Decider {
    @Override
    public String decide(final StepExecution[] executions) throws Exception {
        return "xxx";
    }
}
