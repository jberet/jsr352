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

package org.jberet.support.io;

import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} for reading OOXML Excel files. Current implementation is
 * based on Apache POI XSSF streaming reader API.
 *
 * @see ExcelStreamingItemWriter
 * @since 1.1.0
 */

@Named
@Dependent
public class ExcelStreamingItemReader extends ExcelUserModelItemReader implements ItemReader {
}
