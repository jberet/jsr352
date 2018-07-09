/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.loopback;

import java.io.File;
import java.util.regex.Pattern;
import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.io.PatternFilenameFilter;

@Named
public final class ScanDirectoryBatchlet extends AbstractBatchlet {
    public static final String END = "END";
    public static final String CONTINUE = "CONTINUE";

    @Inject
    @BatchProperty
    private File directory;

    @Inject
    @BatchProperty
    private Pattern pattern;

    @Inject
    private JobContext jobContext;

    @Override
    public String process() throws Exception {
        final File[] files = directory.listFiles(new PatternFilenameFilter(pattern));
        if (files == null || files.length == 0) {
            jobContext.setTransientUserData(null);
            return END;
        }
        jobContext.setTransientUserData(files[0]);
        return CONTINUE;
    }
}
