package umons.ac.be.learner.VPATTT;

import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.learner.Learner;

import java.util.HashSet;

public abstract class AbstractVPATTT<I> implements Learner<I, OneSEVPA<?, I>> {
    protected TTTLearnerVPA<I> learner;
    protected EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle;

    int numberOfEQ = 0;

    @Override
    public OneSEVPA<?, I> constructHypothesis() {
        return learner.getHypothesisModel();
    }

    @Override
    public OneSEVPA<?, I> learn() {
        learner.startLearning();
        OneSEVPA<?, I> hypo;
        while (true) {
            hypo = learner.getHypothesisModel();
            numberOfEQ ++;

            System.out.println("Searching for counterexample " + numberOfEQ);
            System.out.println("Number of nodes: " + learner.getDiscriminationTree().getNodes().size());

            DefaultQuery<I, Boolean> ce = equivalenceOracle.findCounterExample(hypo.getSemantics(), new HashSet<>());

            System.out.println("Found counterexample, processing...");

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
