/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012-2013, Red Hat, Inc., and individual contributors
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

package org.jberet.repository;

public final class JdbcRepository extends AbstractRepository {
    public static final String DDL_FILE_NAME_KEY = "ddl-file";
    public static final String SQL_FILE_NAME_KEY = "sql-file";
    public static final String DATASOURCE_LOOKUP_KEY = "datasource-lookup";

    private JdbcRepository() {

    }

    private static class Holder {
        private static final JdbcRepository instance = new JdbcRepository();
    }

    static JdbcRepository getInstance() {
        return Holder.instance;
    }

    @Override
    long nextJobInstanceId() {
        return 0;
    }

    @Override
    long nextJobExecutionId() {
        return 0;
    }

    @Override
    long nextStepExecutionId() {
        return 0;
    }
}
