/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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
     * <p/>
     * This property can be used at the job parameter for users or for implementations of the SPI at the {@link
     * BatchEnvironment#getBatchConfigurationProperties()}  batch environment} level.
     */
    String LOCAL_TX = "jberet.local-tx";
}
