/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;

public class BatchConfig {
    public static final String CONFIG_FILE_NAME = "jberet.properties";

    private volatile Properties configProperties;

    private BatchConfig() {
    }

    private static class Holder {
        private static final BatchConfig instance = new BatchConfig();
    }

    public static BatchConfig getInstance() {
        return Holder.instance;
    }

    public synchronized void setConfigProperties(final Properties configProperties) {
        this.configProperties = configProperties;
    }

    public Properties getConfigProperties() {
        Properties result = configProperties;
        if(result == null) {
            synchronized (this) {
                result = configProperties;
                if(result == null) {
                    result = new Properties();
                    final InputStream configStream = BatchUtil.getBatchApplicationClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
                    if (configStream != null) {
                        try {
                            result.load(configStream);
                        } catch (IOException e) {
                            throw BatchLogger.LOGGER.failToLoadConfig(e, CONFIG_FILE_NAME);
                        }
                    } else {
                        BatchLogger.LOGGER.useDefaultJBeretConfig(CONFIG_FILE_NAME);
                    }
                    configProperties = result;
                }
            }
        }
        return result;
    }
}
