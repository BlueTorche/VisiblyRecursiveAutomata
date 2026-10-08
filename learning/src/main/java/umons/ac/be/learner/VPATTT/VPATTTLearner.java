package umons.ac.be.learner.VPATTT;

import de.learnlib.acex.AcexAnalyzers;
import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.SEVPA;

public class VPATTTLearner<I> extends AbstractVPATTTLearner<I> {
    public VPATTTLearner(VPAlphabet<I> alphabet,
                                          MembershipOracle.DFAMembershipOracle<I> membershipOracle,
                                          EquivalenceOracle<SEVPA<?, I>, I, Boolean> equivalenceOracle) {
        learner = new TTTLearnerVPA<>(alphabet,
                membershipOracle,
                AcexAnalyzers.LINEAR_FWD);
        this.equivalenceOracle = equivalenceOracle;
    }
}
