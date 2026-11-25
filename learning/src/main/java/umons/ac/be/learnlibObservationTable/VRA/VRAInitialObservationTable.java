package umons.ac.be.learnlibObservationTable.VRA;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import umons.ac.be.learner.NewVRALearner.LearningSymbol;
import umons.ac.be.learnlibObservationTable.AbstractRegularObservationTable;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.*;

public class VRAInitialObservationTable<I>
        extends AbstractRegularObservationTable<I, LearningSymbol<I>, LearningSymbol<I>, Word<LearningSymbol<I>>> {
    public VRAInitialObservationTable(Alphabet<LearningSymbol<I>> alphabet, int query_batch_size) {
        super(alphabet, query_batch_size);
    }

    @Override
    protected void buildQueries(List<DefaultQuery<I, Boolean>> queryList,
                              Word<LearningSymbol<I>> representative,
                              List<Word<LearningSymbol<I>>> separators) {
        int representativeSize = representative.length();
        for (Word<LearningSymbol<I>> separator : separators) {
            WordBuilder<I> wordBuilder = new WordBuilder<>(representativeSize + separator.length());
            for (LearningSymbol<I> symbol : representative) {
                wordBuilder.append(symbol.expand());
            }
            for (LearningSymbol<I> symbol : separator) {
                wordBuilder.append(symbol.expand());
            }
            queryList.add(new DefaultQuery<>(wordBuilder.toWord()));
        }
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> initialize(MembershipOracle<I, Boolean> membershipOracle) {
        return initializeWithSeparators(
                Collections.singletonList(Word.epsilon()),
                Collections.singletonList(Word.epsilon()),
                membershipOracle
        );
    }

    @Override
    protected Word<LearningSymbol<I>> buildSuccessorWord(Word<LearningSymbol<I>> representative, LearningSymbol<I> symbol) {
        return representative.append(symbol);
    }

    public Word<LearningSymbol<I>> getEquivalentRep(Word<LearningSymbol<I>> word) {
        RegularRow<LearningSymbol<I>> row = getRepresentativeRows().getFirst();
        for (LearningSymbol<I> s: word) {
            row = getSuccessor(row, s);
        }
        return row.getLabel();
    }

    public int getNumberStates() {
        return getRepresentativeRows().size();
    }
}
