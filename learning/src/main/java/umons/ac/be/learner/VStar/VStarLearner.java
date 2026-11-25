package umons.ac.be.learner.VStar;

import de.learnlib.acex.AcexAnalyzers;
import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.common.util.Pair;
import net.automatalib.common.util.Triple;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.VStarObservationTables.AbstractInitialObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.CallObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.InitialObservationTable;
import umons.ac.be.learner.Learner;

import java.util.ArrayList;
import java.util.List;

public class VStarLearner<I> extends AbstractVStarLearner<I> {
    public VStarLearner(VPAlphabet<I> alphabet,
                        MembershipOracle<I, Boolean> membershipOracle,
                        EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle,
                        int query_batch_size) {
        learner = new VStar<>(alphabet,
                membershipOracle, query_batch_size);
        this.equivalenceOracle = equivalenceOracle;
    }
}
