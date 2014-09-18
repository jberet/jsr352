/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
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