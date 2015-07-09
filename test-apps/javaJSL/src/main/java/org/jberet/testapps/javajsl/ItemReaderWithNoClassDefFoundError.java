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

import java.util.Arrays;
import java.util.LinkedList;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ItemReaderWithNoClassDefFoundError extends AbstractItemReader {
    @Inject
    @BatchProperty
    private boolean throwError;

    private final LinkedList<Integer> data = new LinkedList<Integer>(Arrays.asList(1, 2, 3, null));

    @Override
    public Object readItem() throws Exception {
        if (throwError) {
            throw new NoClassDefFoundError("NoClassDefFoundError from " + this);
        }
        return data.remove(0);
    }
}
