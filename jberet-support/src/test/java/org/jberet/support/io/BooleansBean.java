/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.support.io;

final public class BooleansBean {
    private boolean boolTrueFalse;
    private boolean bool10;
    private boolean boolyn;
    private boolean booltf;

    private boolean boolYesNo;
    private boolean boolOnOff;
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isBoolTrueFalse() {
        return boolTrueFalse;
    }

    public void setBoolTrueFalse(final boolean boolTrueFalse) {
        this.boolTrueFalse = boolTrueFalse;
    }

    public boolean isBool10() {
        return bool10;
    }

    public void setBool10(final boolean bool10) {
        this.bool10 = bool10;
    }

    public boolean isBoolyn() {
        return boolyn;
    }

    public void setBoolyn(final boolean boolyn) {
        this.boolyn = boolyn;
    }

    public boolean isBooltf() {
        return booltf;
    }

    public void setBooltf(final boolean booltf) {
        this.booltf = booltf;
    }

    public boolean isBoolYesNo() {
        return boolYesNo;
    }

    public void setBoolYesNo(final boolean boolYesNo) {
        this.boolYesNo = boolYesNo;
    }

    public boolean isBoolOnOff() {
        return boolOnOff;
    }

    public void setBoolOnOff(final boolean boolOnOff) {
        this.boolOnOff = boolOnOff;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BooleansBean{");
        sb.append("boolTrueFalse=").append(boolTrueFalse);
        sb.append(", bool10=").append(bool10);
        sb.append(", boolyn=").append(boolyn);
        sb.append(", booltf=").append(booltf);
        sb.append(", boolYesNo=").append(boolYesNo);
        sb.append(", boolOnOff=").append(boolOnOff);
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
