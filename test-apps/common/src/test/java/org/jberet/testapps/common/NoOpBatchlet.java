/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.testapps.common;

import javax.batch.api.Batchlet;
import javax.inject.Named;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class NoOpBatchlet implements Batchlet {

    @Override
    public String process() throws Exception {
        return "Processed";
    }

    @Override
    public void stop() throws Exception {
        // nothing to do
    }
}
