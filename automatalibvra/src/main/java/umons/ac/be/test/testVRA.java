package umons.ac.be.test;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.graph.concept.GraphViewable;
import net.automatalib.util.automaton.builder.AutomatonBuilders;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import umons.ac.be.utils;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.DefaultVRAlphabet;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.HashMap;

public class testVRA {
    public static void main(String[] args) {
        // testVRA();
        testReducedVRA();
    }

    private static void testVRA(){
        VRAlphabet<String> alphabet = buildAlphabet();
        DefaultVRAwithDFA<?, String> vra = buildVRA(alphabet);

        System.out.println("Initial State:" + vra.getInitialState());

        String[] w1 = new String[]{"c1", "i1", "c1", "i2", "i1", "r1", "r1",
                "i1", "c1", "c2", "i1", "r2", "i1", "r1", "i2"};
        System.out.println(vra.accepts(Word.fromArray(w1, 0, w1.length)));

        String[] w2 = new String[]{"c1", "i1", "c1", "i2", "i1", "r1", "r1",
                "i1", "c1", "c1", "i1", "r1", "i1", "r1", "i2"};
        System.out.println(vra.accepts(Word.fromArray(w2, 0, w2.length)));

        Visualization.visualize(vra);
    }

    private static VRAlphabet<String> buildAlphabet(){
        Alphabet<String> internalAlphabet = Alphabets.fromArray("i1", "i2");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c1", "c2");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r1", "r2");

        VRAlphabet<String> vrAlphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        vrAlphabet.addProceduralSymbol("J1", "c1", "r1");
        vrAlphabet.addProceduralSymbol("J2", "c2", "r2");
        vrAlphabet.addProceduralSymbol("J3", "c1", "r1");
        vrAlphabet.addProceduralSymbol("J4", "c1", "r1");
        vrAlphabet.addProceduralSymbol("J5", "c2", "r2");

        return vrAlphabet;
    }

    private static DefaultVRAwithDFA<?, String> buildVRA(VRAlphabet<String> alphabet){
        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("J3").to("s0")
                .from("s0").on("J1").to("s0")
                .from("s0").on("i1").to("s1")
                .from("s0").on("J2").to("s3")
                .from("s1").on("J1").to("s2")
                .from("s2").on("i2").to("s3")
                .withAccepting("s3")
                .create();


        DFA<?, String> J1Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j10")
                .from("j10").on("J2").to("j11")
                .from("j10").on("i2").to("j11")
                .from("j11").on("J1").to("j11")
                .from("j11").on("i1").to("j11")
                .withAccepting("j11")
                .create();


        DFA<?, String> J2Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j20")
                .from("j20").on("i1").to("j21")
                .from("j20").on("i2").to("j21")
                .from("j20").on("J3").to("j21")
                .withAccepting("j21")
                .create();


        DFA<?, String> J3Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j30")
                .from("j30").on("J3").to("j30")
                .from("j30").on("i1").to("j30")
                .from("j30").on("i2").to("j30")
                .withAccepting("j30")
                .create();

        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("J1", J1Procedure);
        procedures.put("J2", J2Procedure);
        procedures.put("J3", J3Procedure);

        return new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure);
    }

    private static DefaultVRAwithDFA<?, String> buildVRA2(VRAlphabet<String> alphabet){
        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("J3").to("s0")
                .from("s0").on("J1").to("s0")
                .from("s0").on("i1").to("s1")
                .from("s0").on("J2").to("s3")
                .from("s1").on("J1").to("s2")
                .from("s2").on("i2").to("s3")
                .from("s1").on("J4").to("s4")
                .from("s1").on("J2").to("s4")
                .from("s2").on("J5").to("s4")
                .from("s3").on("J4").to("s4")
                .from("s4").on("J4").to("s4")
                .from("s4").on("J5").to("s4")
                .from("s4").on("J2").to("s4")
                .withAccepting("s3")
                .create();


        DFA<?, String> J1Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j10")
                .from("j10").on("J2").to("j11")
                .from("j10").on("i2").to("j11")
                .from("j11").on("J1").to("j11")
                .from("j11").on("i1").to("j11")
                .from("j11").on("J3").to("j12")
                .from("j10").on("J4").to("j12")
                .from("j10").on("J5").to("j12")
                .withAccepting("j11")
                .create();


        DFA<?, String> J2Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j20")
                .from("j20").on("i1").to("j21")
                .from("j20").on("i2").to("j21")
                .from("j20").on("J3").to("j21")
                .withAccepting("j21")
                .create();


        DFA<?, String> J3Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j30")
                .from("j30").on("J3").to("j30")
                .from("j30").on("i1").to("j30")
                .from("j30").on("i2").to("j30")
                .withAccepting("j30")
                .create();

        DFA<?, String> J4Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j30")
                .from("j30").on("J3").to("j30")
                .from("j30").on("i1").to("j30")
                .from("j30").on("i2").to("j30")
                .withAccepting("j30")
                .create();

        DFA<?, String> J5Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j30")
                .from("j30").on("J3").to("j30")
                .from("j30").on("i1").to("j30")
                .from("j30").on("i2").to("j30")
                .withAccepting("j30")
                .create();

        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("J1", J1Procedure);
        procedures.put("J2", J2Procedure);
        procedures.put("J3", J3Procedure);
        procedures.put("J4", J4Procedure);
        procedures.put("J5", J5Procedure);

        return new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure);
    }

    private static void testReducedVRA(){
        VRAlphabet<String> alphabet = buildAlphabet();
        VRA<?, String, ?> vra = buildVRA2(alphabet);
        Visualization.visualize(vra);
        Visualization.visualize(vra.removeBinStatesAndAutomata());
    }
}
