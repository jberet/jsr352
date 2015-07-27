/*
 * Copyright (c) 2013-2015 Red Hat, Inc. and/or its affiliates.
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
 * The purpose of the generic type is to remember the calling builder type and instance, which can be returned by
 * these terminating methods:
 * <ul>
 * <li>{@link org.jberet.job.model.Transition.End#exitStatus(String...)}
 * <li>{@link org.jberet.job.model.Transition.Stop#exitStatus(String...)}
 * <li>{@link org.jberet.job.model.Transition.Fail#exitStatus(String...)}
 * <li>{@link org.jberet.job.model.Transition.Next#to(String)}
 * </ul>
 *
 * @param <T> the builder class of the job element type that encloses the current transition element
 */
public abstract class Transition<T> implements Serializable {
    private static final long serialVersionUID = -112488607616329302L;

    private String on;

    transient T enclosingBuilder;

    Transition(final String on) {
        this.on = on;
    }

    /**
     * Gets the {@code on} attribute value.
     * @return {@code on} attribute value
     */
    public String getOn() {
        return on;
    }

    /**
     * Sets the {@code on} attribute value to the specified exit status condition.
     *
     * @param on exit status condition to be set to {@code on} attribute, may contain ? or * wildcard
     */
    void setOn(final String on) {
        this.on = on;
    }

    /**
     * Base class of all terminating transition elements, such as {@link org.jberet.job.model.Transition.End},
     * {@link org.jberet.job.model.Transition.Stop}, and {@link org.jberet.job.model.Transition.Fail}.
     *
     * @param <T> the builder class of the job element type that encloses the current transition element
     */
    public static abstract class Termination<T> extends Transition<T> {
        private static final long serialVersionUID = 4417648893108466995L;
        private String exitStatus;

        Termination(final String on) {
            super(on);
        }

        /**
         * Gets the {@code exit-status} attribute value, which is the new exit status after matching the current
         * transition element.
         *
         * @return the new, updated exit status
         */
        public final String getExitStatus() {
            return exitStatus;
        }

        /**
         * Sets the {@code exit-status} attribute value, which is the new exit status after matching the current
         * transition element.
         *
         * @param exitStatus the new, updated exit stauts
         */
        void setExitStatus(final String exitStatus) {
            this.exitStatus = exitStatus;
        }

        /**
         * Sets the {@code exit-status} attribute value, which is the new exit status after matching the current
         * transition element, and returns the current builder, such as
         * {@link StepBuilder}, {@link DecisionBuilder}, or {@link FlowBuilder}.
         * <p/>
         * If no need for a new exit status, invoking this method without argument to simply exit operation on this
         * transition element and return the enclosing builder.
         *
         * @param newExitStatus the new, updated exit status (optional param and may be omitted)
         * @return the current builder
         */
        public T exitStatus(final String... newExitStatus) {
            if (newExitStatus.length > 0) {
                this.exitStatus = newExitStatus[0];
            }
            final T builder = enclosingBuilder;
            enclosingBuilder = null;
            return builder;
        }
    }

    /**
     * Transition element {@code end}.
     *
     * @param <T> the builder class of the job element type that encloses the current transition element
     */
    public static final class End<T> extends Termination<T> {
        private static final long serialVersionUID = -6145098395052085455L;

        /**
         * Constructs {@code End} instance with specified {@code on} exit status condition.
         *
         * @param on exit status condition (may contain ? or * wildcard)
         */
        End(final String on) {
            super(on);
        }
    }

    /**
     * Transition element {@code fail}.
     *
     * @param <T> the builder class of the job element type that encloses the current transition element
     */
    public static final class Fail<T> extends Termination<T> {
        private static final long serialVersionUID = -5653099756045482389L;

        /**
         * Constructs {@code Fail} instance with specified {@code on} exit status condition.
         *
         * @param on exit status condition (may contain ? or * wildcard)
         */
        Fail(final String on) {
            super(on);
        }
    }

    /**
     * Transition element {@code stop}.
     *
     * @param <T> the builder class of the job element type that encloses the current transition element
     */
    public static final class Stop<T> extends Termination<T> {
        private static final long serialVersionUID = -460513093260191729L;
        private String restart;

        /**
         * Constructs {@code Stop} instance with specified {@code on} exit status condition and {@code restart}
         * job element id.
         *
         * @param on exit status condition (may contain ? or * wildcard)
         * @param restart job element id to restart from, may be null
         */
        Stop(final String on, final String restart) {
            super(on);
            this.restart = restart;
        }

        /**
         * Gets the {@code restart} attribute value, which should points to a job element to restart from.
         *
         * @return {@code restart} attribute value
         */
        public String getRestart() {
            return restart;
        }

        /**
         * Sets the {@code restart} attribute value, which should points to a job element to restart from.
         *
         * @param restart {@code restart} attribute value
         */
        void setRestart(final String restart) {
            this.restart = restart;
        }

        /**
         * Sets the {@code restart} attribute value, which should points to a job element to restart from, and returns
         * the current builder, such as {@link StepBuilder}, {@link DecisionBuilder}, or {@link FlowBuilder}.
         *
         * @param restartFrom {@code restart} attribute value
         * @return the current builder
         */
        public Stop<T> restartFrom(final String restartFrom) {
            this.restart = restartFrom;
            return this;
        }
    }

    /**
     * Transition element {@code next}.
     *
     * @param <T> the builder class of the job element type that encloses the current transition element
     */
    public static final class Next<T> extends Transition<T> {
        private static final long serialVersionUID = 6985540748982496047L;
        private String to;

        /**
         * Constructs {@code Next} instance with specified {@code on} exit status condition.
         *
         * @param on exit status condition (may contain ? or * wildcard)
         */
        Next(final String on) {
            super(on);
        }

        /**
         * Gets the {@code to} attribute value, which should points to the next job element.
         *
         * @return the {@code to} attribute value
         */
        public String getTo() {
            return to;
        }

        /**
         * Sets the {@code to} attribute value, which should points to the next job element.
         * @param to the {@code to} attribute value
         */
        void setTo(final String to) {
            this.to = to;
        }

        /**
         * Sets the {@code to} attribute value, which should points to the next job element, and returns the current
         * builder, such as {@link StepBuilder}, {@link DecisionBuilder}, or {@link FlowBuilder}.
         *
         * @param nextToRun next job element to run
         * @return the current builder instance
         */
        public T to(final String nextToRun) {
            this.to = nextToRun;
            final T builder = enclosingBuilder;
            enclosingBuilder = null;
            return builder;
        }
    }
}
