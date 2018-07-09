/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.jdbcBatchletReaderWriter;

import java.util.HashMap;
import java.util.Map;
import javax.batch.api.chunk.ItemProcessor;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * An item processor that performs any necessary data conversion.
 *
 * @since 1.3.0.Final
 */
@Named
@Dependent
public class ConversionItemProcessor implements ItemProcessor {
    @Override
    public Object processItem(final Object item) throws Exception {
        if (item == null) {
            return item;
        }

        final Map<String, Object> m = new HashMap<>();

        // add data to map...

        return m;
    }
}
