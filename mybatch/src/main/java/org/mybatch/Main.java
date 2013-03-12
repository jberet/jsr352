/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchRuntime;

import org.mybatch.util.ConcurrencyService;

import static org.mybatch.util.BatchLogger.LOGGER;

public class Main {
    private static final long WAIT_FOR_COMPLETION_MILLIS = 5000;

    public static void main(String[] args) throws JobStartException, JobSecurityException {
        if (args.length == 0) {
            usage(args);
        }
        String jobXml = args[0];
        if (jobXml == null || jobXml.isEmpty()) {
            usage(args);
        }

        try {
            BatchRuntime.getJobOperator().start(jobXml, System.getProperties());
        } finally {
            try {
                Thread.sleep(WAIT_FOR_COMPLETION_MILLIS);
                ConcurrencyService.shutdown();
                ConcurrencyService.getExecutorService().awaitTermination(WAIT_FOR_COMPLETION_MILLIS, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    private static void usage(String[] args) {
        LOGGER.mainUsage(Arrays.asList(args));
    }
}
