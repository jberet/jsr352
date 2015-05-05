/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * Extends {@link StockTradeBase} with a {@code org.joda.time.DateTime} field, which should be able to be serialized and
 * deserialized registering jackson-datatype-joda module.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class StockTradeWithJoda extends StockTradeBase {
    private static final long serialVersionUID = -8633645369476783517L;

    @NotNull
    @Past
    @JsonProperty("Date")
    DateTime date;

    public DateTime getDate() {
        return date;
    }

    public void setDate(final DateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StockTrade{");
        sb.append("date=").append(date);
        sb.append(", time='").append(time).append('\'');
        sb.append(", open=").append(open);
        sb.append(", high=").append(high);
        sb.append(", low=").append(low);
        sb.append(", close=").append(close);
        sb.append(", volume=").append(volume);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof StockTradeWithJoda)) return false;

        final StockTradeWithJoda that = (StockTradeWithJoda) o;

        if (Double.compare(that.close, close) != 0) return false;
        if (Double.compare(that.high, high) != 0) return false;
        if (Double.compare(that.low, low) != 0) return false;
        if (Double.compare(that.open, open) != 0) return false;
        if (Double.compare(that.volume, volume) != 0) return false;
        if (!date.equals(that.date)) return false;
        if (!time.equals(that.time)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = date.hashCode();
        result = 31 * result + time.hashCode();
        temp = Double.doubleToLongBits(open);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(high);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(low);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(close);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(volume);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
