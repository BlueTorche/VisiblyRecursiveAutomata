package umons.ac.be.learnlibObservationTable.Row;

import net.automatalib.word.Word;

import java.util.ArrayList;
import java.util.List;

public class RegularRow<I> implements Row<I> {
    protected final Word<I> label;
    private int contentID;
    private List<RegularRow<I>> successors;

    public RegularRow(Word<I> label) {
        this.label = label;
    }

    public RegularRow(Word<I> label, int alphabetSize) {
        this.label = label;
        successors = new ArrayList<>(alphabetSize);
    }

    public void setContent(int id) {
        contentID = id;
    }

    public int getContentId() {
        return contentID;
    }

    public void addSuccessor(RegularRow<I> row) {
        this.successors.add(row);
    }

    public RegularRow<I> getSuccessor(int idx) {
        return successors.get(idx);
    }

    public Word<I> getLabel() {
        return label;
    }

    public boolean isRepresentative() {
        return successors != null && !successors.isEmpty();
    }

    public void setRepresentative(int alphabetSize) {
        successors = new ArrayList<>(alphabetSize);
    }

    @Override
    public String toString() {
        return label.toString() + "\t|\t" + contentID;
    }
}
