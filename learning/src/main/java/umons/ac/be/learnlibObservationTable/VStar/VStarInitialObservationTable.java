package umons.ac.be.learnlibObservationTable.VStar;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.word.Word;
import umons.ac.be.learnlibObservationTable.AbstractRegularObservationTable;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.Collections;
import java.util.List;

public class VStarInitialObservationTable<I> extends AbstractRegularObservationTable<I, Word<I>, I, Word<I>> {
    public VStarInitialObservationTable(Alphabet<Word<I>> alphabet, int query_batch_size) {
        super(alphabet, query_batch_size);
    }

    @Override
    protected void buildQueries(List<DefaultQuery<I, Boolean>> queryList, Word<I> representative, List<Word<I>> separators) {
        for (Word<I> s : separators) {
            queryList.add(new DefaultQuery<>(representative, s));
        }
    }

    @Override
    public List<RegularRow<I>> initialize(MembershipOracle<I, Boolean> membershipOracle) {
        return initializeWithSeparators(
                Collections.singletonList(Word.epsilon()),
                Collections.singletonList(Word.epsilon()),
                membershipOracle
        );
    }

    @Override
    protected Word<I> buildSuccessorWord(Word<I> representative, Word<I> symbol) {
        return Word.fromWords(representative, symbol);
    }
}
