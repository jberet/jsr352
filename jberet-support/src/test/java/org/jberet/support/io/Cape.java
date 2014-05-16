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

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A bean class that represents stock market data used in "Irrational Exuberance" by Robert J. Shiller.
 * The Excel file, ie_data.xls, is downloaded from www.econ.yale.edu/~shiller/data/ie_data.xls
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Cape implements Serializable {
    private static final long serialVersionUID = -1345054340595353167L;

    double date;
    double sp;
    double dividend;
    double earnings;
    double cpi;
    double dateFraction;
    double longInterestRate;
    double realPrice;
    double realDividend;
    double realEarnings;
    String cape;    //Cyclically Adjusted Price Earnings Ratio P/E10 or CAPE

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Cape{");
        sb.append("date=").append(date);
        sb.append(", sp=").append(sp);
        sb.append(", dividend=").append(dividend);
        sb.append(", earnings=").append(earnings);
        sb.append(", cpi=").append(cpi);
        sb.append(", dateFraction=").append(dateFraction);
        sb.append(", longInterestRate=").append(longInterestRate);
        sb.append(", realPrice=").append(realPrice);
        sb.append(", realDividend=").append(realDividend);
        sb.append(", realEarnings=").append(realEarnings);
        sb.append(", cape=").append(cape);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Cape)) return false;

        final Cape cape1 = (Cape) o;

        if (Double.compare(cape1.cpi, cpi) != 0) return false;
        if (Double.compare(cape1.date, date) != 0) return false;
        if (Double.compare(cape1.dateFraction, dateFraction) != 0) return false;
        if (Double.compare(cape1.dividend, dividend) != 0) return false;
        if (Double.compare(cape1.earnings, earnings) != 0) return false;
        if (Double.compare(cape1.longInterestRate, longInterestRate) != 0) return false;
        if (Double.compare(cape1.realDividend, realDividend) != 0) return false;
        if (Double.compare(cape1.realEarnings, realEarnings) != 0) return false;
        if (Double.compare(cape1.realPrice, realPrice) != 0) return false;
        if (Double.compare(cape1.sp, sp) != 0) return false;
        if (cape != null ? !cape.equals(cape1.cape) : cape1.cape != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(date);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(sp);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(dividend);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(earnings);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(cpi);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(dateFraction);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longInterestRate);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(realPrice);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(realDividend);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(realEarnings);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (cape != null ? cape.hashCode() : 0);
        return result;
    }
}
