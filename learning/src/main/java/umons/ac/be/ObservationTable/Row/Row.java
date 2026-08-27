package umons.ac.be.ObservationTable.Row;

import umons.ac.be.ObservationTable.Row.Content.Content;
import net.automatalib.word.Word;

public interface Row<I> {
    Word<I> getPrefix();

    void setSuccessorRow(int succIdx, Row<I> row);

    boolean equivalentTo(Row<I> other);

    int getDistinctionSeparator(Row<I> other);

    boolean isPrime();

    Row<I> getParent();

    void setParent(Row<I> row);

    Row<I> getSuccessor(int idx);

    boolean isAccepting() ;

    Content getContent();

    int consistentTo(Row<I> other);

    void addSeparator();

    boolean notEmpty();
}
