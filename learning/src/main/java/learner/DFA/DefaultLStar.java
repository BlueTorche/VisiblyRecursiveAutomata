package learner.DFA;

import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.DFA;
import umons.ac.be.oracle.rl.Oracle;

public class DefaultLStar<I> extends AbstractLStar<I> {
    public DefaultLStar(GrowingAlphabet<I> alphabet, Oracle<I, DFA<?, I>> oracle) {
        super(alphabet, oracle);
    }
}
