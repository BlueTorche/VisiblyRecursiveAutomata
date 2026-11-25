package umons.ac.be.learnlibObservationTable.Row;

import net.automatalib.word.Word;

public interface Row<I> {
    void setContent(int id);

    int getContentId();

    Word<I> getLabel();
}
