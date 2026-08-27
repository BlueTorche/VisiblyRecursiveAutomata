package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.ObservationTable;
import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.word.Word;

import java.util.Map;

public interface RecursiveObservationTable<I> extends ObservationTable<I, Map<Word<I>, FastDFA<I>>> {
    FastDFA<I> constructIndividualHypothesis(Word<I> procSymbol);

    boolean procComplete();

    boolean areRecursiveEquivalent(Word<I> recEquivClass, Word<I> representative, int separatorIdx);

    void fetchMembershipContext(Row<I> row, int idx, int contIdx);

    void addContext(Word<I> prefix, Word<I> suffix);

    Word<I> getRecursiveEquivalent(Word<I> word);

    Row<I> createNewEmptyRow(Word<I> prefix);
}
