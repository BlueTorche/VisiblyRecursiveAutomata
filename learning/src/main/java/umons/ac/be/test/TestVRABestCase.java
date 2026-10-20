package umons.ac.be.test;

import de.learnlib.oracle.EquivalenceOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.visualization.Visualization;
import umons.ac.be.learner.Learner;
import umons.ac.be.learner.VPATTT.VPATTTLearner;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import umons.ac.be.learner.VRALearner.VRAIsomorphicLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearnerOptimized;
import umons.ac.be.learner.VStar.VStarLearner;
import umons.ac.be.oracle.vpl.BestCaseForVRAConformance;
import umons.ac.be.oracle.vpl.BestCaseForVRAMembership;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestVRABestCase {
    public static void main(String[] args) {
        int n = 4;
        int k = 3;
        int ITER = 100_000;

        List<String> intAlpha = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            intAlpha.add("a" + i);
        }

        VPAlphabet<String> alph = new DefaultVPAlphabet<>(
                Alphabets.fromCollection(intAlpha),
                Alphabets.singleton("c"),
                Alphabets.singleton("r")
        );



        BestCaseForVRAMembership membershipOracle = new BestCaseForVRAMembership(alph, n);
        System.out.println("Oracle Size: " + membershipOracle.getVRA().size());

        BestCaseForVRAConformance<VRA<FastDFAState, String, DFA<FastDFAState, String>>> equivalenceOracle =
                new BestCaseForVRAConformance<>(membershipOracle, n, ITER, new Random(0));


        final AbstractVRALearner<String> learner = new VRASeparateLearnerOptimized<>(
                    VRAlphabet.fromVPAlphabet(alph), membershipOracle, equivalenceOracle);


        float startTime = System.nanoTime();
        VRA<?, String, DFA<FastDFAState, String>> learnedVRA = learner.learn();
        float runtime = (System.nanoTime() - startTime) / 1_000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        learner.displayStats();
        System.out.println("Learned Size: " + learnedVRA.removeBinStatesAndAutomata().size());

//        learner.printTables();
//        Visualization.visualize(learnedVRA);
//        Visualization.visualize(learnedVRA.removeBinStatesAndAutomata());


        membershipOracle = new BestCaseForVRAMembership(alph, n);
        BestCaseForVRAConformance<SEVPA<?, String>> equivalenceOracleVPA =
                new BestCaseForVRAConformance<>(membershipOracle, n, ITER, new Random(0));


        final Learner<String, ? extends DeterministicAcceptorTS<?, String>> learnerTTT =  new VPATTTLearner<>(
                alph,
                membershipOracle,
                equivalenceOracleVPA
        );

//        startTime = System.nanoTime();
//        OneSEVPA<?, String> learnedOneSEVPA = (OneSEVPA<?, String>) learnerTTT.learn();
//        runtime = (System.nanoTime() - startTime) / 1_000_000_000;
//        System.out.println("Total learning time : " + runtime+ " s");
//        learnerTTT.displayStats();
//        System.out.println("Number of membership queries: " + membershipOracle.getNumberOfMQ());
//        System.out.println("Learned Size: " + (learnedOneSEVPA.size()-1));

        membershipOracle = new BestCaseForVRAMembership(alph, n);

        final Learner<String, ? extends DeterministicAcceptorTS<?, String>> learnerVStar =  new VStarLearner<>(
                alph,
                membershipOracle,
                equivalenceOracleVPA
        );

        startTime = System.nanoTime();
        SEVPA<?, String> learnedSEVPA = (SEVPA<?, String>) learnerVStar.learn();
        runtime = (System.nanoTime() - startTime) / 1_000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        learnerVStar.displayStats();
        System.out.println("Number of membership queries: " + membershipOracle.getNumberOfMQ());
        System.out.println("Learned Size: " + (learnedSEVPA.size()-1));
    }
}
