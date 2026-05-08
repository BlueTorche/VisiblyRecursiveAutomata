package learner;

import net.automatalib.word.Word;

public interface ObservationTable<I> {
    void closeTable();

    void addRepresentative(Word<I> r);

    void addSeparator(Word<I> s);

    boolean makeTableSigmaConsistent();

    void makeTableClosedConsistent();

    void processCounterExample(Word<I> cx);

    void initialize();
}
