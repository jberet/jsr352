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
public abstract class Transition implements Serializable {
    private static final long serialVersionUID = -112488607616329302L;

    private final String on;

    Transition(String on) {
        this.on = on;
    }

    public String getOn() {
        return on;
    }

    public static abstract class Termination extends Transition {
        private static final long serialVersionUID = 4417648893108466995L;
        private String exitStatus;

        Termination(String on) {
            super(on);
        }

        public final String getExitStatus() {
            return exitStatus;
        }

        void setExitStatus(String exitStatus) {
            this.exitStatus = exitStatus;
        }
    }

    public static final class End extends Termination {
        private static final long serialVersionUID = -6145098395052085455L;

        End(String on) {
            super(on);
        }
    }

    public static final class Fail extends Termination {
        private static final long serialVersionUID = -5653099756045482389L;

        Fail(String on) {
            super(on);
        }
    }

    public static final class Stop extends Termination {
        private static final long serialVersionUID = -460513093260191729L;
        private final String restart;

        Stop(String on, String restart) {
            super(on);
            this.restart = restart;
        }

        public String getRestart() {
            return restart;
        }
    }

    public static final class Next extends Transition {
        private static final long serialVersionUID = 6985540748982496047L;
        private String to;

        Next(String on) {
            super(on);
        }

        public String getTo() {
            return to;
        }

        void setTo(String to) {
            this.to = to;
        }
    }
}
