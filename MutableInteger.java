/*
 * A barebones mutable integer implementation providing the functionality
 * needed for LFUCache
 */
public class MutableInteger extends Number implements Comparable<MutableInteger> {
    private int value;

    MutableInteger(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void incrementValue() {
        value++;
    }

    public int compareTo(MutableInteger m) {
        return Integer.compare(value, m.value);
    }

    @Override 
    public int hashCode() {
        return value;
    }

    @Override 
    public boolean equals(Object o) {
        if (!(o instanceof MutableInteger)) {
            return false;
        }

        MutableInteger m = (MutableInteger) o;
        return value == m.value;

    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public byte byteValue() {
        return (byte) value;
    }

    @Override
    public short shortValue() {
        return (short) value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public double doubleValue() {
        return (double) value;
    }
}
