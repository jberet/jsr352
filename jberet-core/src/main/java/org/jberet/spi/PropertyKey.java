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
}
