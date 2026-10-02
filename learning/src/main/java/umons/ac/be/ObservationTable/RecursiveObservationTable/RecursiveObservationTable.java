package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.ObservationTable;
import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.word.Word;

import java.util.BitSet;
import java.util.Map;

public interface RecursiveObservationTable<I> extends ObservationTable<I, Map<Word<I>, FastDFA<I>>> {
    FastDFA<I> constructIndividualHypothesis(Word<I> procSymbol);

    boolean procComplete();


    void fetchMembershipContext(Row<I> row, int idx, int contIdx);

    void addContext(Word<I> prefix, Word<I> suffix);

    Word<I> getRecursiveEquivalent(Word<I> word);

    Word<I> getRecursiveEquivalent(Word<I> word, int separatorIdx);

    boolean areRecursiveEquivalent(BitSet recEquivClass, Word<I> representative, int separatorIdx);

    boolean isRecursivePrime(Word<I> word, int separatorIdx);

    Row<I> createNewEmptyRow(Word<I> prefix);

    Map<Word<I>, BitSet> getRecursiveEquivalentClass();
}
