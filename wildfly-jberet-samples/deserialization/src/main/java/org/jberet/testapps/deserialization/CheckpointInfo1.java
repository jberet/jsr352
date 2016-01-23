package org.jberet.testapps.deserialization;

import java.io.Serializable;

public final class CheckpointInfo1 implements Serializable {
    private static final long serialVersionUID = 1L;

    private int number;

    public CheckpointInfo1(final int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(final int number) {
        this.number = number;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckpointInfo1)) return false;

        final CheckpointInfo1 that = (CheckpointInfo1) o;

        if (number != that.number) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return number;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CheckpointInfo1{");
        sb.append("number=").append(number);
        sb.append('}');
        return sb.toString();
    }
}
