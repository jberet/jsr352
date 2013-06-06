/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
 
package org.jberet.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jberet.repository.JobRepositoryFactory;
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

    public synchronized void setConfigProperties(Properties configProperties) {
        this.configProperties = configProperties;
    }

    public Properties getConfigProperties() {
        Properties result = configProperties;
        if(result == null) {
            synchronized (this) {
                result = configProperties;
                if(result == null) {
                    result = new Properties();
                    InputStream configStream = BatchUtil.getBatchApplicationClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
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
