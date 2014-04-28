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

import java.io.Serializable;

/**
 * Java bean class for a record in fixed length file Entity File from
 * http://star.cde.ca.gov/star2013/research_fixfileformat.aspx
 */
public class CaliforniaStarEntity implements Serializable {
    private static final long serialVersionUID = 1212747443304548794L;

    private String countyCode;        // 2
    private String districtCode;      // 5
    private String schoolCode;        // 7
    private String charterNumber;     // 4
    private int testYear;             // 4
    private String typeId;            // 2
    private String countyName;        // 50
    private String districtName;      // 50
    private String schoolName;        // 50
    private String zipCode;           // 5

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getCountyCode() {
        return countyCode;
    }

    public void setCountyCode(final String countyCode) {
        this.countyCode = countyCode;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(final String districtCode) {
        this.districtCode = districtCode;
    }

    public String getSchoolCode() {
        return schoolCode;
    }

    public void setSchoolCode(final String schoolCode) {
        this.schoolCode = schoolCode;
    }

    public String getCharterNumber() {
        return charterNumber;
    }

    public void setCharterNumber(final String charterNumber) {
        this.charterNumber = charterNumber;
    }

    public int getTestYear() {
        return testYear;
    }

    public void setTestYear(final int testYear) {
        this.testYear = testYear;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(final String typeId) {
        this.typeId = typeId;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(final String countyName) {
        this.countyName = countyName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(final String districtName) {
        this.districtName = districtName;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(final String schoolName) {
        this.schoolName = schoolName;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(final String zipCode) {
        this.zipCode = zipCode;
    }
}
