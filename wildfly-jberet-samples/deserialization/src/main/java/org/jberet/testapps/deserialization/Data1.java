package org.jberet.testapps.deserialization;

import java.io.Serializable;

public final class Data1 implements Serializable {
    private static final long serialVersionUID = 1L;

    String value;

    public Data1(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Data1{");
        sb.append("value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
