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

package org.jberet.testapps.common;

import java.util.Arrays;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

@Named("integerProcessor")
public class IntegerProcessor extends IntegerArrayReaderWriterProcessorBase implements ItemProcessor {

    @Override
    public Object processItem(final Object item) throws Exception {
        if (failOnValues != null && Arrays.binarySearch(failOnValues, item) >= 0 &&
                (repeatFailure || !failedValues.contains(item))) {
            failedValues.add((Integer) item);
            throw new ArithmeticException("Failing on value " + failOnValues);
        }

        return item;
    }
}
