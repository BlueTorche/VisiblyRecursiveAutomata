package learner.DFA;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import oracle.Oracle;

public class DefaultLStar<I> extends AbstractLStar<I> {
    public DefaultLStar(Alphabet<I> alphabet, Oracle<I, DFA<?, I>> oracle) {
        super(alphabet, oracle);
    }
}
