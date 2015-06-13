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

package org.jberet.job.model;

import java.io.Serializable;

/**
 * Base class for all transition elements: end, fail, next and stop.
 */
public abstract class Transition<T> implements Serializable {
    private static final long serialVersionUID = -112488607616329302L;

    private String on;

    transient T enclosingBuilder;

    Transition(final String on) {
        this.on = on;
    }

    public String getOn() {
        return on;
    }

    void setOn(final String on) {
        this.on = on;
    }

    public static abstract class Termination<T> extends Transition<T> {
        private static final long serialVersionUID = 4417648893108466995L;
        private String exitStatus;

        Termination(final String on) {
            super(on);
        }

        public final String getExitStatus() {
            return exitStatus;
        }

        void setExitStatus(final String exitStatus) {
            this.exitStatus = exitStatus;
        }

        public T exitStatus(final String... newExitStatus) {
            if (newExitStatus.length > 0) {
                this.exitStatus = newExitStatus[0];
            }
            final T builder = enclosingBuilder;
            enclosingBuilder = null;
            return builder;
        }
    }

    public static final class End<T> extends Termination<T> {
        private static final long serialVersionUID = -6145098395052085455L;

        End(final String on) {
            super(on);
        }
    }

    public static final class Fail<T> extends Termination<T> {
        private static final long serialVersionUID = -5653099756045482389L;

        Fail(final String on) {
            super(on);
        }
    }

    public static final class Stop<T> extends Termination<T> {
        private static final long serialVersionUID = -460513093260191729L;
        private String restart;

        Stop(final String on, final String restart) {
            super(on);
            this.restart = restart;
        }

        public String getRestart() {
            return restart;
        }

        void setRestart(final String restart) {
            this.restart = restart;
        }

        public Stop<T> restartFrom(final String restartFrom) {
            this.restart = restartFrom;
            return this;
        }
    }

    public static final class Next<T> extends Transition<T> {
        private static final long serialVersionUID = 6985540748982496047L;
        private String to;

        Next(final String on) {
            super(on);
        }

        public String getTo() {
            return to;
        }

        void setTo(final String to) {
            this.to = to;
        }

        public T to(final String to) {
            this.to = to;
            final T builder = enclosingBuilder;
            enclosingBuilder = null;
            return builder;
        }
    }
}
