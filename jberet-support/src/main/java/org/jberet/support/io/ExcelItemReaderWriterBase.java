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

import javax.batch.api.BatchProperty;
import javax.inject.Inject;

/**
 * The base class of Excel reader and writer classes: {@link ExcelUserModelItemReader} and
 * {@link ExcelItemWriter}.
 *
 * @see ExcelUserModelItemReader
 * @see ExcelItemWriter
 * @since 1.0.3
 */
public abstract class ExcelItemReaderWriterBase extends JsonItemReaderWriterBase {

    @Inject
    @BatchProperty
    protected Class beanType;

    @Override
    protected void registerModule() throws Exception {
        //noop
    }
}
