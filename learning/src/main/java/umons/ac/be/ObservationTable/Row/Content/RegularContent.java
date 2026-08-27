package umons.ac.be.ObservationTable.Row.Content;

import java.util.BitSet;

public class RegularContent implements Content<Integer>{
    BitSet content = new BitSet();

    public void set(Integer idx) {
        content.set(idx);
    }

    public boolean get(Integer idx) {
        return content.get(idx);
    }

    public int getSeparator(Content<Integer> other) {
        BitSet val = (BitSet) content.clone();
        val.xor(((RegularContent) other).content);
        return val.nextSetBit(0);
    }

    @Override
    public boolean isAccepting() {
        return content.get(0);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !other.getClass().equals(RegularContent.class))
            return false;
        return content.equals(((RegularContent) other).content);
    }

    @Override
    public String toString() {
        return content.toString();
    }


    @Override
    public boolean notEmpty() {
        return !content.isEmpty();
    }
}
