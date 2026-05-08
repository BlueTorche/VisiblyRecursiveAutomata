package oracle;

import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.word.Word;

public interface Oracle<I, M> {
    boolean MembershipQuery(Word<I> word);

    Word<I> EquivalenceQuery(M hypothesis);
}
