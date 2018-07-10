/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package groovy

import groovy.transform.Field

@Field List<String[]> rows;
@Field int position = 0;

def open(checkpoint) {
    String resourcePath = batchProperties.get("resource");
    InputStream inputFile = this.class.getClassLoader().getResourceAsStream(resourcePath);

    String[] lines = inputFile.text.split('\n');
    rows = lines.collect { it.split(',') };
    inputFile.close();

    if (checkpoint != null) {
        position = checkpoint;
    }
    println("ItemReader.groovy open, rows: " + rows);
}

def checkpointInfo() {
    return position;
}

def readItem() {
    if (position >= rows.size()) {
        return null;
    }
    return rows.get(position++);
}
