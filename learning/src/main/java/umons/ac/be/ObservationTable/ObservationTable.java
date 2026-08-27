package umons.ac.be.ObservationTable;

import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.word.Word;

public interface ObservationTable<I, M> {

    void addRepresentative(Word<I> r);

    void addSeparator(Word<I> s);

    boolean close();

    boolean consistent();

    void enforce();

    void initialize();

    M constructHypothesis();

    Word<I> getInconsistentSeparator(Row<I> row1, Row<I> row2);

    Row<I> createNewRow(Word<I> r);

    void createNewColumn(int idx);

    void fetchMembership(Row<I> row, int idx);

    void checkRowPrime(Row<I> row);

    void checkNewPrimes();

    void addSymbol(I symbol);

    Word<I> getParent(Word<I> word);
}
