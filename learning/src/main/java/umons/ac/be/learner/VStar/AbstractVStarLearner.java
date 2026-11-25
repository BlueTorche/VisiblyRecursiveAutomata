package umons.ac.be.learner.VStar;

import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import de.learnlib.query.Query;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.automaton.vpa.impl.DefaultNSEVPA;
import net.automatalib.automaton.vpa.impl.Location;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.VStarObservationTables.AbstractCallObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.AbstractInitialObservationTable;
import umons.ac.be.learner.Learner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class AbstractVStarLearner<I> implements Learner<I, SEVPA<?, I>> {
    protected VStar<I> learner;
    protected EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle;

    int numberOfEQ = 0;

    @Override
    public SEVPA<?, I> constructHypothesis() {
        return learner.getHypothesisModel();
    }

    @Override
    public SEVPA<?, I> learn() {
        learner.startLearning();
        SEVPA<?, I> hypo;
        while (true) {
            hypo = learner.getHypothesisModel();
            numberOfEQ ++;

            System.out.println("Searching for counterexample " + numberOfEQ);
//            learner.printTables();

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
