/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.spi;

/**
 * Keys used for JBeret specific configuration and job properties.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface PropertyKey {

    /**
     * A key used to indicate transactions should be kept local and not use the global transaction. The value for this
     * key can be {@code true} or {@code false}.
     * This property can be used at the job parameter for users or for implementations of the SPI at the {@link
     * BatchEnvironment#getBatchConfigurationProperties()}  batch environment} level.
     */
    String LOCAL_TX = "jberet.local-tx";

    /**
     * A key used to specify the resource path to the configuration XML for configuring infinispan job repository.
     */
    String INFINISPAN_XML = "infinispan-xml";

    /**
     * A key used to specify the number of seconds the split execution will wait for its constituent flows to complete.
     * This key can be optionally used in job parameters when starting or restarting a job execution, or configured as
     * a job property in job XML.
     * When present in both places, the job parameter has higher precedence.
     * <p>
     * Its value should be a positive integer.
     * If this amount of time elapses before all flows complete, the split execution will fail with
     * {@code javax.batch.operations.BatchRuntimeException}.
     * If no such job parameter or job property is present, the split execution will wait infinitely for its flows to
     * complete.
     */
    String SPLIT_TIMEOUT_SECONDS = "jberet.split.timeout.seconds";

    /**
     * A key used to specify restart mode as an optional restart job parameter.
     * Valid values are:
     * <ul>
     * <li>strict: only job executions with FAILED or STOPPED batch status can be restarted;
     * <li>force: will restart job execution with FAILED, STOPPED, STARTING, STARTED, or STOPPING batch status;
     * <li>detect: will restart job executions with FAILED or STOPPED batch status, and will restart job executions with
     *             STARTING, STARTED, or STOPPING batch status only if they are detected to have terminated, which might
     *             have been caused by JVM crash or processed been killed. {@code detect} is the default value.
     * </ul>
     */
    String RESTART_MODE = "jberet.restart.mode";

    String RESTART_MODE_STRICT = "strict";
    String RESTART_MODE_FORCE = "force";
    String RESTART_MODE_DETECT = "detect";

    /**
     * A key used to disable transactions around calls to {@link PartitionAnalyzer}.
     * Transactions around calls to {@link PartitionAnalyzer} might time out in long running steps which can
     * be prevented by disabling the transaction using this property.
     */
    String ANALYZER_TX_DISABLED = "jberet.analyzer.txDisabled";

}
