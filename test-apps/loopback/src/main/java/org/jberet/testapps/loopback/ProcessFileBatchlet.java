/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.loopback;

import java.io.File;
import java.nio.charset.Charset;
import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.io.Files;

@Named
public final class ProcessFileBatchlet extends AbstractBatchlet {
    @Inject
    private JobContext jobContext;

    @Override
    public String process() throws Exception {
        final File file = (File) jobContext.getTransientUserData();
        jobContext.setTransientUserData(null);
        final String firstLine = Files.readFirstLine(file, Charset.defaultCharset());
        System.out.printf("1st line of %s: %s%n", file.getPath(), firstLine);

        if (file.delete()) {
            System.out.printf("Finished processing, and deleted file %s%n", file.getPath());
        }

        return null;
    }
}
