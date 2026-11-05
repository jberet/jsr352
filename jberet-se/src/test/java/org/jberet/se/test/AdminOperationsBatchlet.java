/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import java.util.ArrayList;
import java.util.List;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jberet.operations.ExtendedJobOperator;
import org.jberet.operations.FailExecutionsResult;
import org.jberet.operations.FailOptions;
import org.jberet.operations.StaleExecutionsPage;
import org.jboss.logging.Logger;

/**
 * Reaps stale executions of a target job and optionally restarts them.
 */
@Named
@Dependent
public class AdminOperationsBatchlet extends AbstractBatchlet {

    private static final Logger LOGGER = Logger.getLogger(AdminOperationsBatchlet.class);

    /** Exit status when the operator in use is not a JBeret one. */
    public static final String UNSUPPORTED = "UNSUPPORTED";

    /** Exit status when the target job has no stale executions. */
    public static final String NOTHING_STALE = "NOTHING_STALE";

    @Inject
    @BatchProperty(name = "targetJobName")
    private String targetJobName;

    @Inject
    @BatchProperty(name = "staleMinutes")
    private String staleMinutes;

    @Inject
    @BatchProperty(name = "pageSize")
    private String pageSize;

    @Inject
    @BatchProperty(name = "restartAfterFail")
    private String restartAfterFail;

    @Inject
    @BatchProperty(name = "exitStatusOnFail")
    private String exitStatusOnFail;

    @Inject
    @BatchProperty(name = "selfProtect")
    private String selfProtect;

    @Inject
    private JobContext jobContext;

    @Override
    public String process() throws Exception {
        final JobOperator operator = BatchRuntime.getJobOperator();
        if (!(operator instanceof ExtendedJobOperator)) {
            //not a JBeret operator - degrade, do not crash
            jobContext.setExitStatus(UNSUPPORTED);
            return UNSUPPORTED;
        }
        final ExtendedJobOperator admin = (ExtendedJobOperator) operator;

        final int minutes = staleMinutes == null ? 0 : Integer.parseInt(staleMinutes.trim());
        final int size = pageSize == null ? 100 : Integer.parseInt(pageSize.trim());

        final StaleExecutionsPage page =
                admin.listExecutionsNotUpdatedInMinutes(targetJobName, minutes, 0, size);
        LOGGER.infof("Stale executions of %s: %s of %s", targetJobName, page.getExecutionIds(),
                page.getTotalCount());
        if (page.getExecutionIds().isEmpty()) {
            jobContext.setExitStatus(NOTHING_STALE);
            return NOTHING_STALE;
        }

        final List<Long> ids = new ArrayList<Long>(page.getExecutionIds());
        if (selfProtect == null || Boolean.parseBoolean(selfProtect.trim())) {
            ids.remove(Long.valueOf(jobContext.getExecutionId()));
        }

        FailOptions options = FailOptions.defaults();
        if (exitStatusOnFail != null && !exitStatusOnFail.trim().isEmpty()) {
            options = options.withExitStatus(exitStatusOnFail.trim());
        }
        final FailExecutionsResult result = admin.failJobExecutions(ids, options);

        if (Boolean.parseBoolean(restartAfterFail)) {
            for (final Long id : result.getFailedExecutionIds()) {
                try {
                    admin.restart(id, null);        //FAILED is restartable
                } catch (final Exception e) {
                    //one bad restart must not abort the reap
                    LOGGER.warnf(e, "Failed to restart job execution %s", id);
                    jobContext.setExitStatus("PARTIAL_RESTART");
                    return "PARTIAL_RESTART";
                }
            }
        }

        final String status = "FAILED=" + result.getFailedCount()
                + ";SKIPPED=" + result.getSkippedCount()
                + ";REMAINING=" + (page.getTotalCount() - result.getFailedCount());
        jobContext.setExitStatus(status);
        return status;
    }
}
