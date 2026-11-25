package umons.ac.be.learner.NewVRALearner;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import umons.ac.be.learner.Learner;
import umons.ac.be.vra.VRA;

import java.util.HashSet;

public abstract class AbstractVRALearner<I> implements Learner<I, VRA<?, I, ?>> {
    protected AbstractVRAStar<I> learner;
    protected EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle;

    int numberOfEQ = 0;

    @Override
    public VRA<?, I, ?> constructHypothesis() {
        return learner.getHypothesisModel();
    }

    @Override
    public VRA<?, I, ?> learn() {
        learner.startLearning();
        VRA<?, I, ?> hypo;
        while (true) {
            hypo = learner.getHypothesisModel();
            numberOfEQ ++;
//            Visualization.visualize(hypo);

//             learner.printTables();
            System.out.println("Searching for counterexample " + numberOfEQ);

            DefaultQuery<I, Boolean> ce = equivalenceOracle.findCounterExample(hypo.getSemantics(), new HashSet<>());

            System.out.println("Found counterexample, processing...");
            // System.out.println(ce);

            if (ce == null) {
                break;
            }
            final boolean refined = learner.refineHypothesis(ce);
            assert refined;
        }
        return hypo;
    }

    @Override
    public void processCounterExample(Word<I> cx) {
        assert false;
    }

    @Override
    public boolean askMembershipQuery(Word<I> word) {
        assert false;
        return false;
    }

    @Override
    public void displayStats() {
        System.out.println("Number of equivalence queries: " + numberOfEQ);
    }

    @Override
    public int getNumberOfEQ() {
        return numberOfEQ;
    }
}
