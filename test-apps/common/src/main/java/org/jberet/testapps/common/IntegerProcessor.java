/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
