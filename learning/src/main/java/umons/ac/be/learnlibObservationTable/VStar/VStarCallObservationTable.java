package umons.ac.be.learnlibObservationTable.VStar;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.learnlibObservationTable.AbstractRegularObservationTable;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.List;

public class VStarCallObservationTable<I> extends AbstractRegularObservationTable<I, Word<I>, I, Pair<Word<I>, Word<I>>> {
    public VStarCallObservationTable(Alphabet<Word<I>> alphabet, int query_batch_size) {
        super(alphabet, query_batch_size);
    }

    @Override
    protected void buildQueries(List<DefaultQuery<I, Boolean>> queryList, Word<I> representative, List<Pair<Word<I>, Word<I>>> separators) {
        for (Pair<Word<I>,Word<I>> c : separators) {
            queryList.add(new DefaultQuery<>(
                    Word.fromWords(c.getFirst(), representative, c.getSecond())
            ));
        }
    }

    @Override
    public List<RegularRow<I>> initialize(MembershipOracle<I, Boolean> membershipOracle) {
        assert false;
        return null;
    }

    @Override
    protected Word<I> buildSuccessorWord(Word<I> representative, Word<I> symbol) {
        return Word.fromWords(representative, symbol);
    }
}
