package learner.ObservationTable.Row;

import net.automatalib.word.Word;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class RegularRow<I> implements Row<I> {
    private final Word<I> prefix;
    protected Content contents;
    protected final List<Row<I>> successorRows = new ArrayList<>();
    private Row<I> equivalentParent = null;


    public RegularRow(Word<I> prefix) {
        this.prefix = prefix;
        contents = new RegularContent();
    }

    public Word<I> getPrefix() {
        return prefix;
    }

    public void setSuccessorRow(int succIdx, Row<I> row) {
        while (successorRows.size() <= succIdx) {
            successorRows.add(null);
        }
        successorRows.set(succIdx, row);
    }

    public boolean equivalentTo(Row<I> other) {
        return contents.equals(((RegularRow<I>) other).contents);
    }

    public int consistentTo(Row<I> other) {
        for (int idx = 0; idx < successorRows.size(); idx++) {
            if (!successorRows.get(idx).equivalentTo(other.getSuccessor(idx))) {
                return idx;
            }
        }
        return -1;
    }

    @Override
    public void addSeparator() { }

    public int getDistinctionSeparator(Row<I> other) {
        return contents.getSeparator(other.getContent());
    }

    public boolean isPrime() {
        return equivalentParent == null;
    }

    public Row<I> getParent() {
        return equivalentParent;
    }

    public void setParent(Row<I> row) {
        equivalentParent = row;
    }

    public Row<I> getSuccessor(int idx) {
        return successorRows.get(idx);
    }

    public boolean isAccepting() {
        return contents.isAccepting();
    }

    public Content getContent() {
        return contents;
    }

    public void fetchContent(int colIdx) {
        contents.set(colIdx);
    }

    @Override
    public String toString() {
        return prefix + "\t| " + contents + "\t| " + (isPrime() ? "" : equivalentParent.getPrefix());
    }
}
