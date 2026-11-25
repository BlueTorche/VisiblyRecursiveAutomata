package umons.ac.be.learnlibObservationTable.VRA;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import umons.ac.be.learner.NewVRALearner.LearningSymbol;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.*;

public interface RecursiveObservationTable<I> {
    default void buildQueries(List<DefaultQuery<I, Boolean>> queryList,
                                Word<LearningSymbol<I>> representative,
                                List<Word<LearningSymbol<I>>> separators,
                                List<Pair<Word<I>, Word<I>>> contexts) {
        Pair<I, I> callReturnSymbol = getCallReturnSymbols();

        for (Word<LearningSymbol<I>> separator : separators) {
            WordBuilder<I> wordBuilder = new WordBuilder<>();
            for (LearningSymbol<I> symbol : representative) {
                wordBuilder.append(symbol.expand());
            }
            for (LearningSymbol<I> symbol : separator) {
                wordBuilder.append(symbol.expand());
            }
            Word<I> expandedWord = wordBuilder.toWord();
            for (Pair<Word<I>, Word<I>> context : contexts) {
                queryList.add(new DefaultQuery<>(Word.fromWords(
                        context.getFirst(),
                        Word.fromLetter(callReturnSymbol.getFirst()),
                        expandedWord,
                        Word.fromLetter(callReturnSymbol.getSecond()),
                        context.getSecond()
                )));
            }
        }
    }

    default void buildQueries(List<DefaultQuery<I, Boolean>> queryList,
                              Word<LearningSymbol<I>> representative,
                              Word<LearningSymbol<I>> separator,
                              List<Pair<Word<I>, Word<I>>> contexts) {
        buildQueries(queryList, representative, Collections.singletonList(separator), contexts);
    }

    default void buildQueries(List<DefaultQuery<I, Boolean>> queryList,
                              Word<LearningSymbol<I>> representative,
                              List<Word<LearningSymbol<I>>> separators,
                              Pair<Word<I>, Word<I>> context) {
        buildQueries(queryList, representative, separators, Collections.singletonList(context));
    }

    default List<RegularRow<LearningSymbol<I>>> initialize(MembershipOracle<I, Boolean> membershipOracle) {
        return initialize(
                Collections.singletonList(Word.epsilon()),
                Collections.singletonList(Word.epsilon()),
                Collections.emptyList(),
                membershipOracle
        );
    }

    default boolean checkPrefixClosed(Collection<? extends Word<LearningSymbol<I>>> initialShortPrefixes) {
        Set<Word<LearningSymbol<I>>> prefixes = new HashSet<>(initialShortPrefixes);

        for (Word<LearningSymbol<I>> pref : initialShortPrefixes) {
            if (!pref.isEmpty() && !prefixes.contains(pref.prefix(-1))) {
                return false;
            }
        }

        return true;
    }

    default void fetchResults(
            Iterator<DefaultQuery<I, Boolean>> queryIt,
            BitSet output,
            int idxContext) {
        if (queryIt.next().getOutput()) {
            output.set(idxContext);
        }
    }

    default void fetchRowResult(Iterator<DefaultQuery<I, Boolean>> queryIt,
                                int numSeparators,
                                int numContexts,
                                List<BitSet> rowContents) {
        for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
            for (int idxContext = 0; idxContext < numContexts; idxContext++) {
                fetchResults(queryIt, rowContents.get(idxSep), idxContext);
            }
        }
    }

    default List<BitSet> fetchInitialRowResult(Iterator<DefaultQuery<I, Boolean>> queryIt,
                                               int numSeparators,
                                               int numContexts) {
        List<BitSet> rowContents = new ArrayList<>(numSeparators);
        for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
            rowContents.add(new BitSet(numContexts));
        }
        fetchRowResult(queryIt, numSeparators, numContexts, rowContents);
        return rowContents;
    }

    default List<BitSet> cloneRowContent(List<BitSet> rowContents) {
        List<BitSet> result = new ArrayList<>(rowContents.size());
        for (BitSet content : rowContents) {
            result.add((BitSet) content.clone());
        }
        return result;
    }

    default Word<LearningSymbol<I>> buildSuccessorWord(Word<LearningSymbol<I>> representative,
                                                       LearningSymbol<I> symbol) {
        return representative.append(symbol);
    }


    List<RegularRow<LearningSymbol<I>>> initialize(
            List<Word<LearningSymbol<I>>> initialRepresentative,
            List<Word<LearningSymbol<I>>> initialSeparators,
            List<Pair<Word<I>, Word<I>>> initialContexts,
            MembershipOracle<I, Boolean> membershipOracle);

    List<RegularRow<LearningSymbol<I>>> toRepresentative(
            List<RegularRow<LearningSymbol<I>>> representative,
            MembershipOracle<I, Boolean> membershipOracle);

    List<RegularRow<LearningSymbol<I>>> addSeparator(
            Word<LearningSymbol<I>> newSeparator,
            MembershipOracle<I, Boolean> membershipOracle);

    List<RegularRow<LearningSymbol<I>>> addContext(
            Pair<Word<I>, Word<I>> newContext,
            MembershipOracle<I, Boolean> membershipOracle);

    List<RegularRow<LearningSymbol<I>>> addAlphabetSymbols(
            List<LearningSymbol<I>> newSymbols,
            MembershipOracle<I, Boolean> membershipOracle);

    List<RegularRow<LearningSymbol<I>>> createNewRows(
            List<RegularRow<LearningSymbol<I>>> newRows,
            MembershipOracle<I, Boolean> membershipOracle);

    RegularRow<LearningSymbol<I>> createRepresentativeRow(Word<LearningSymbol<I>> prefix);

    Map<Word<LearningSymbol<I>>, BitSet> getNewProceduralSymbols();

    boolean processContents(RegularRow<LearningSymbol<I>> row, List<BitSet> rowContents);

    Pair<I, I> getCallReturnSymbols();

    BitSet getBitsetValue(Word<LearningSymbol<I>> word, MembershipOracle<I, Boolean> membershipOracle);

    List<RegularRow<LearningSymbol<I>>> getRepresentativeRows();

    List<Word<LearningSymbol<I>>> getSeparators();

    int getNumberSymbols();
}
