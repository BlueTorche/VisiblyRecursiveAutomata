package umons.ac.be.ObservationTable.Row;

import umons.ac.be.ObservationTable.Row.Content.RecursiveContent;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.BitSet;

public class IsomorphicRecursiveRow<I> extends RegularRow<I> {
    public IsomorphicRecursiveRow(Word<I> prefix, int separatorSize) {
        super(prefix);
        contents = new RecursiveContent(separatorSize);
    }

    public void fetchContent(int colIdx, int contIdx) {
        contents.set(Pair.of(colIdx, contIdx));
    }

    public boolean equivalentTo(Row<I> other) {
        return contents.equals(((IsomorphicRecursiveRow<I>) other).contents);
    }

    public BitSet getBaseContext() {
        return ((RecursiveContent) contents).getContentVal(0);
    }

    @Override
    public void addSeparator() {
        ((RecursiveContent) contents).addSeparator();
    }
}
