package learner.ObservationTable.Row;

import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.BitSet;

public class IsomorphicRecursiveRow<I> extends RegularRow<I> {
    public IsomorphicRecursiveRow(Word<I> prefix) {
        super(prefix);
        contents = new RecursiveContent();
    }

    public void fetchContent(int colIdx, int contIdx) {
        contents.set(Pair.of(colIdx, contIdx));
    }
}
