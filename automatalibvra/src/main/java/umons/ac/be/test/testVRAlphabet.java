package umons.ac.be.test;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import umons.ac.be.vraalphabet.DefaultVRAlphabet;
import umons.ac.be.vraalphabet.VRAlphabet;

public class testVRAlphabet {
    public static void main(String[] args) {
        testVRAlphabet();
    }

    public static void testVRAlphabet() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray("i1", "i2");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c1", "c2");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r1", "r2");

        VRAlphabet<String> vrAlphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        vrAlphabet.addProceduralSymbol("J1", "c1", "r1");
        vrAlphabet.addProceduralSymbol("J2", "c2", "r2");
        vrAlphabet.addProceduralSymbol("J3", "c1", "r1");

        System.out.println(vrAlphabet.getAutomatonAlphabet());
        System.out.println(vrAlphabet.getInputAlphabet());
        System.out.println(vrAlphabet);
        System.out.println(vrAlphabet.getSymbol(3));
        System.out.println(vrAlphabet.getSymbolIndex("J1"));
        System.out.println(vrAlphabet.getProceduralAlphabetFromCallAndReturn("c1", "r1"));

        System.out.println(vrAlphabet.isProceduralSymbol("J1"));
        System.out.println(vrAlphabet.isProceduralSymbol("i1"));
    }
}
