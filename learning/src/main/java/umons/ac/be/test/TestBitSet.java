package umons.ac.be.test;

import java.util.BitSet;

public class TestBitSet {
    public static void main(String[] args) {
        BitSet x1 = new BitSet();
        BitSet x2 = new BitSet();
        System.out.println(x1 + " " + x2);
        x1.set(0);
        x2.set(1);
        System.out.println(x1 + " " + x2);
        x1.xor(x2);
        System.out.println(x1 + " " + x2);
    }
}
