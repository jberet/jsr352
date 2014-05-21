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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A bean class that represents company data.
 * The CSV file, companylist.csv, is downloaded from
 * http://www.nasdaq.com/screening/companies-by-name.aspx?letter=0&exchange=nasdaq&render=download
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Company implements Serializable {
    private static final long serialVersionUID = -4680584766804470416L;
    @JsonProperty("Symbol")
    String symbol;

    @JsonProperty("Name")
    String name;

    @JsonProperty("LastSale")
    double lastSale;

    @JsonProperty("MarketCap")
    double marketCap;

    @JsonProperty("ADR TSO")
    String address;

    @JsonProperty("IPOyear")
    String ipoYear;

    @JsonProperty("Sector")
    String sector;

    @JsonProperty("industry")
    String industry;

    @JsonProperty("Summary Quote")
    String summaryQuote;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public double getLastSale() {
        return lastSale;
    }

    public void setLastSale(final double lastSale) {
        this.lastSale = lastSale;
    }

    public double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(final double marketCap) {
        this.marketCap = marketCap;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public String getIpoYear() {
        return ipoYear;
    }

    public void setIpoYear(final String ipoYear) {
        this.ipoYear = ipoYear;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(final String sector) {
        this.sector = sector;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(final String industry) {
        this.industry = industry;
    }

    public String getSummaryQuote() {
        return summaryQuote;
    }

    public void setSummaryQuote(final String summaryQuote) {
        this.summaryQuote = summaryQuote;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Company{");
        sb.append("symbol='").append(symbol).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", lastSale=").append(lastSale);
        sb.append(", marketCap=").append(marketCap);
        sb.append(", address='").append(address).append('\'');
        sb.append(", ipoYear='").append(ipoYear).append('\'');
        sb.append(", sector='").append(sector).append('\'');
        sb.append(", industry='").append(industry).append('\'');
        sb.append(", summaryQuote='").append(summaryQuote).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Company)) return false;

        final Company company = (Company) o;

        if (Double.compare(company.lastSale, lastSale) != 0) return false;
        if (Double.compare(company.marketCap, marketCap) != 0) return false;
        if (address != null ? !address.equals(company.address) : company.address != null) return false;
        if (industry != null ? !industry.equals(company.industry) : company.industry != null) return false;
        if (ipoYear != null ? !ipoYear.equals(company.ipoYear) : company.ipoYear != null) return false;
        if (!name.equals(company.name)) return false;
        if (sector != null ? !sector.equals(company.sector) : company.sector != null) return false;
        if (summaryQuote != null ? !summaryQuote.equals(company.summaryQuote) : company.summaryQuote != null)
            return false;
        if (!symbol.equals(company.symbol)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = symbol.hashCode();
        result = 31 * result + name.hashCode();
        temp = Double.doubleToLongBits(lastSale);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(marketCap);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (ipoYear != null ? ipoYear.hashCode() : 0);
        result = 31 * result + (sector != null ? sector.hashCode() : 0);
        result = 31 * result + (industry != null ? industry.hashCode() : 0);
        result = 31 * result + (summaryQuote != null ? summaryQuote.hashCode() : 0);
        return result;
    }
}
