/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.upsertWriter;

import java.util.Map;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

@Named
public class UpperCaseItemProcessor implements ItemProcessor {
    public static final String TITLE_KEY = "tit";

    @Override
    public Object processItem(final Object item) throws Exception {
        final Map<String, String> itemAsMap = (Map<String, String>) item;
        final String title = itemAsMap.get(TITLE_KEY);
        if (title != null) {
            itemAsMap.put(TITLE_KEY, title.toUpperCase());
        }
        return itemAsMap;
    }
}