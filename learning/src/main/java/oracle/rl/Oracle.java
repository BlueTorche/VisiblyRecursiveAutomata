package oracle.rl;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.word.Word;

public interface Oracle<I, M> {
    boolean MembershipQuery(Word<I> word);

    Word<I> EquivalenceQuery(M hypothesis);

    void displayStats();

    Alphabet<I> getInputAlphabet();
}
