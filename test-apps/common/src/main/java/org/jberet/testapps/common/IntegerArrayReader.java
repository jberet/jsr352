/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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

import javax.batch.api.chunk.ItemReader;
import javax.inject.Named;

@Named("integerArrayReader")
public final class IntegerArrayReader extends IntegerArrayReaderWriterBase implements ItemReader {
    @Override
    public Object readItem() throws Exception {
        if (cursor > partitionEnd || cursor < partitionStart) {
            return null;
        }
        if (cursor == readerFailAt && (repeatFailure || cursor != failurePointRemembered)) {
            failurePointRemembered = readerFailAt;
            cursor++;
            throw new ArithmeticException("Failing at reader.fail.at point " + readerFailAt);
        }
        final Integer result = data[cursor];
        cursor++;
        return result;
    }

    @Override
    protected void initData() {
        super.initData();
        for (int i = 0; i < dataCount; i++) {
            data[i] = i;
        }
        //position the cursor according to partition start
        cursor = partitionStart;
        System.out.printf("Partition start = %s, end = %s in %s%n", partitionStart, partitionEnd, this);
    }
}
