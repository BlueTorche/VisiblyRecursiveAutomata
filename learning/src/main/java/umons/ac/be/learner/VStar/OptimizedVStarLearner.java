package umons.ac.be.learner.VStar;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.word.Word;

public class OptimizedVStarLearner<I> extends AbstractVStarLearner<I> {
    public OptimizedVStarLearner(VPAlphabet<I> alphabet,
                              MembershipOracle<I, Boolean> membershipOracle,
                              EquivalenceOracle<SEVPA<?, I>, I, Boolean> equivalenceOracle) {
        this.alphabet = alphabet;
        this.membershipOracle = membershipOracle;
        this.equivalenceOracle = equivalenceOracle;
    }

    @Override
    public void processCounterExample(Word<I> cx) {
        // TODO
    }
}
