package org.samples.wildfly.jberet.common;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A bean class that represents stock trade data. Copied from project jberet-support.
 * The CSV file, IBM_unadjusted.txt, is downloaded from kibot.com free trial data:
 * http://www.kibot.com/buy.aspx
 * http://api.kibot.com/?action=history&symbol=IBM&interval=1&unadjusted=1&bp=1&user=guest
 *
 * The data is in standard CSV format with order of fields:
 * Date,Time,Open,High,Low,Close,Volume
 *
 * The data file contains no header line.  The data file is truncated to 10/13/2008,12:09 to stay within Excel row
 * number limit (1048576 rows max).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class StockTrade implements Serializable {
    private static final long serialVersionUID = -55209987994863611L;
    @JsonProperty("Date")
    Date date;

    @JsonProperty("Time")
    String time;

    @JsonProperty("Open")
    double open;

    @JsonProperty("High")
    double high;

    @JsonProperty("Low")
    double low;

    @JsonProperty("Close")
    double close;

    @JsonProperty("Volume")
    double volume;

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(final String time) {
        this.time = time;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(final double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(final double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(final double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(final double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(final double volume) {
        this.volume = volume;
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
        if (!(o instanceof StockTrade)) return false;

        final StockTrade that = (StockTrade) o;

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
