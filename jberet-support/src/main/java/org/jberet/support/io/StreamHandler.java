/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.support.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A handler which allows the streams from a {@link Process} to be consumed.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface StreamHandler {

    /**
     * Sets the {@link OutputStream} to handle {@code stdin}.
     *
     * @param stdin the output stream that handles standard input
     *
     * @throws IOException if an I/O error occurs
     */
    void setProcessInputStream(OutputStream stdin) throws IOException;

    /**
     * Sets the {@link InputStream} to handle {@code stdout}.
     *
     * @param stdout the input stream that handles standard out
     *
     * @throws IOException if an I/O error occurs
     */
    void setProcessOutputStream(InputStream stdout) throws IOException;

    /**
     * Sets the {@link InputStream} to handle {@code stderr}.
     *
     * @param stderr the input stream that handles standard error
     *
     * @throws IOException if an I/O error occurs
     */
    void setProcessErrorStream(InputStream stderr) throws IOException;

    /**
     * Start handling of the streams.
     */
    void start();

    /**
     * Stop handling the streams.
     */
    void stop();
}
