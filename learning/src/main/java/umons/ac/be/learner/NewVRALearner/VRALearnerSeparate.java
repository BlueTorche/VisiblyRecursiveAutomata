package umons.ac.be.learner.NewVRALearner;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

public class VRALearnerSeparate<I> extends AbstractVRALearner<I> {
    public VRALearnerSeparate(VPAlphabet<I> alphabet,
                              MembershipOracle<I, Boolean> membershipOracle,
                              EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle,
                              int query_batch_size) {
        learner = new VRAStarSeparate<>(alphabet, membershipOracle, query_batch_size);
        this.equivalenceOracle = equivalenceOracle;
    }
}
