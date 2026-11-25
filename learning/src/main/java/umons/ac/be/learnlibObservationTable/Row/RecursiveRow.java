package umons.ac.be.learnlibObservationTable.Row;

import net.automatalib.word.Word;

import java.util.ArrayList;
import java.util.List;

public class RecursiveRow<I> extends RegularRow<I> {
    private final List<Integer> contentID = new ArrayList<>();

    public RecursiveRow(Word<I> label) {
        super(label);
    }

    public RecursiveRow(Word<I> label, int alphabetSize) {
        super(label, alphabetSize);
    }

    public int getContentID(int index) {
        return contentID.get(index);
    }

    public void setContent(int index, int contentID) {
        this.contentID.set(index, contentID);
    }

    @Override
    public String toString() {
        return label.toString() + "\t|\t" + contentID;
    }
}
