package umons.ac.be.learner.VPATTT;

import de.learnlib.acex.AcexAnalyzers;
import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

public class VPATTT<I> extends AbstractVPATTT<I> {
    public VPATTT(VPAlphabet<I> alphabet,
                  MembershipOracle<I, Boolean> membershipOracle,
                  EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle) {
        learner = new TTTLearnerVPA<>(alphabet,
                membershipOracle,
                AcexAnalyzers.LINEAR_FWD);
        this.equivalenceOracle = equivalenceOracle;
    }
}
