package umons.ac.be.learner;

import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;

public interface Learner<I, M extends DeterministicAcceptorTS<?, I>> {
    M constructHypothesis();

    M learn();

    void processCounterExample(Word<I> cx);

    boolean askMembershipQuery(Word<I> word);

    void displayStats();
}
