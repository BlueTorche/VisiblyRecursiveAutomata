package umons.ac.be.test;

import learner.VRA.VRALearner;
import learner.VRA.isomophicLearning.VRAIsomorphicLearner;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.vpa.impl.DefaultOneSEVPA;
import net.automatalib.automaton.vpa.impl.Location;
import net.automatalib.util.automaton.builder.AutomatonBuilders;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import oracle.vpl.VPLOracleFromOneSEVPAWithConformance;
import oracle.vpl.VPLOracleFromVRAWithConformance;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vraalphabet.DefaultVRAlphabet;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.HashMap;

public class TestLearningVRA {
    public static void main(String[] args) {
        // testLearningIsomoprhicWithOneSEVPA1();
        testLearningIsomorphicWithVRA();
    }

    private static void testLearningIsomorphicWithOneSEVPA1() {
        VPAlphabet<String> alphabet = new DefaultVPAlphabet<String>(
                Alphabets.fromArray("i1", "i2"), Alphabets.fromArray("c"), Alphabets.fromArray("r")
        );
        final DefaultOneSEVPA<String> oneSEVPA = new DefaultOneSEVPA<>(alphabet);

        final Location l0 = oneSEVPA.addInitialLocation(false);
        final Location l1 = oneSEVPA.addLocation(true);

        oneSEVPA.setInternalSuccessor(l0, "i1", l1);
        oneSEVPA.setInternalSuccessor(l1, "i2", l0);

        oneSEVPA.setReturnSuccessor(l0, "r", oneSEVPA.encodeStackSym(l1, "c"), l1);
        oneSEVPA.setReturnSuccessor(l1, "r", oneSEVPA.encodeStackSym(l0, "c"), l1);

        Visualization.visualize(oneSEVPA);

        final VPLOracleFromOneSEVPAWithConformance<String, AbstractVRAwithDFA<?, String>> oracle =
                new VPLOracleFromOneSEVPAWithConformance<>(alphabet, oneSEVPA);

        final VRALearner<String> learner = new VRAIsomorphicLearner<>(VRAlphabet.fromVPAlphabet(alphabet), oracle);

        AbstractVRAwithDFA<?, String> vra = learner.learn();
        Visualization.visualize(vra.removeBinStatesAndAutomata());

    }

    public static void testLearningIsomorphicWithVRA() {
        VRAlphabet<String> alphabet = buildAlphabet();
        DefaultVRAwithDFA<?, String> vra = buildVRA(alphabet);
        Visualization.visualize(vra);

        final VPLOracleFromVRAWithConformance<String, AbstractVRAwithDFA<?, String>> oracle =
                new VPLOracleFromVRAWithConformance<>(alphabet, vra);

        final VRALearner<String> learner = new VRAIsomorphicLearner<>(VRAlphabet.fromVPAlphabet(alphabet), oracle);

        AbstractVRAwithDFA<?, String> learnedVRA = learner.learn();
        oracle.displayStats();

//        Visualization.visualize(learnedVRA);
//        Visualization.visualize(learnedVRA.removeBinStatesAndAutomata());

        Word<String> w = Word.fromSymbols("c2", "c1", "c1", "r2", "r1", "r2");
        System.out.println(w + "\t" + learnedVRA.accepts(w) + "\t" + vra.accepts(w));
    }

    private static VRAlphabet<String> buildAlphabet(){
        Alphabet<String> internalAlphabet = Alphabets.fromArray("i1", "i2");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c1", "c2");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r1", "r2");

        VRAlphabet<String> vrAlphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        vrAlphabet.addProceduralSymbol("J1", "c1", "r1");
        vrAlphabet.addProceduralSymbol("J2", "c2", "r2");
        vrAlphabet.addProceduralSymbol("J3", "c1", "r1");

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
}
