package learner.ObservationTable;

import net.automatalib.automaton.Automaton;
import net.automatalib.word.Word;

public interface ObservationTable<I> {

    void addRepresentative(Word<I> r);

    void addSeparator(Word<I> s);

    boolean close();

    boolean consistent();

    void enforce();

    void initialize();

    Automaton<?, I, ?> constructHypothesis();
}
