package umons.ac.be.learnlibObservationTable.VRA;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.learner.NewVRALearner.LearningSymbol;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.*;

public abstract class AbstractRecursiveObservationTable<I> implements RecursiveObservationTable<I> {
    protected final List<Word<LearningSymbol<I>>> separators = new ArrayList<>();
    protected final Set<Word<LearningSymbol<I>>> separatorsSet = new HashSet<>();

    protected final List<Pair<Word<I>, Word<I>>> contexts = new ArrayList<>();
    protected final Set<Pair<Word<I>, Word<I>>> contextsSet = new HashSet<>();

    protected final List<RegularRow<LearningSymbol<I>>> representativeRows = new ArrayList<>();
    protected final List<RegularRow<LearningSymbol<I>>> successorsRows = new ArrayList<>();
    protected final Map<Word<LearningSymbol<I>>, RegularRow<LearningSymbol<I>>> allRows = new HashMap<>();

    protected final List<List<BitSet>> rowContents = new ArrayList<>();
    protected final Map<List<BitSet>, Integer> rowContentsIds = new HashMap<>();

    protected final Alphabet<LearningSymbol<I>> alphabet;
    protected final Pair<I, I> callReturnSymbol;

    protected final List<BitSet> primeContext = new ArrayList<>();
    protected final List<Word<LearningSymbol<I>>> proceduralSymbolsWord = new ArrayList<>();
    protected final List<Word<LearningSymbol<I>>> newProceduralSymbols = new ArrayList<>();
    protected final Map<BitSet, Integer> primeContextIds  = new HashMap<>();

    protected final int QUERY_BATCH_SIZE;

    protected AbstractRecursiveObservationTable(Alphabet<LearningSymbol<I>> alphabet,
                                                I callSymbol, I returnSymbol,
                                                int query_batch_size) {
        this.alphabet = new GrowingMapAlphabet<>(alphabet);
        this.callReturnSymbol = Pair.of(callSymbol, returnSymbol);
        this.QUERY_BATCH_SIZE = query_batch_size;
    }

    @Override
    public Pair<I, I> getCallReturnSymbols() {
        return callReturnSymbol;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> getRepresentativeRows() {
        return representativeRows;
    }

    @Override
    public List<Word<LearningSymbol<I>>> getSeparators() {
        return separators;
    }

    public boolean isFinal(RegularRow<LearningSymbol<I>> row, BitSet bitSet) {
        return rowContents.get(row.getContentId()).getFirst().equals(bitSet);
    }

    @Override
    public BitSet getBitsetValue(Word<LearningSymbol<I>> word, MembershipOracle<I, Boolean> membershipOracle) {
        if (allRows.containsKey(word)) {
            return rowContents.get(allRows.get(word).getContentId()).getFirst();
        }

        List<DefaultQuery<I, Boolean>> queryList = new ArrayList<>();
        buildQueries(queryList, word, Word.epsilon(), contexts);
        membershipOracle.processQueries(queryList);

        BitSet results = new BitSet();
        for (int i = 0; i < queryList.size(); i++) {
            if (queryList.get(i).getOutput()) {
                results.set(i);
            }
        }
        return results;
    }

    @Override
    public Map<Word<LearningSymbol<I>>, BitSet> getNewProceduralSymbols() {
        Map<Word<LearningSymbol<I>>, BitSet> toReturn = new HashMap<>();
        for (Word<LearningSymbol<I>> word: newProceduralSymbols) {
            toReturn.put(word,
                    rowContents.get(allRows.get(word).getContentId()).getFirst());
        }
        newProceduralSymbols.clear();
        return toReturn;
    }

    @Override
    public RegularRow<LearningSymbol<I>> createRepresentativeRow(Word<LearningSymbol<I>> prefix) {
        if (allRows.containsKey(prefix)) {
            return allRows.get(prefix);
        }
        RegularRow<LearningSymbol<I>> newRow = new RegularRow<>(prefix, alphabet.size());
        allRows.put(prefix, newRow);
        return newRow;
    }

    public RegularRow<LearningSymbol<I>> createSuccessorRow(Word<LearningSymbol<I>> prefix) {
        if (allRows.containsKey(prefix)) {
            return allRows.get(prefix);
        }
        RegularRow<LearningSymbol<I>> newRow = new RegularRow<>(prefix);
        allRows.put(prefix, newRow);
        return newRow;
    }


    @Override
    public List<RegularRow<LearningSymbol<I>>> toRepresentative(
            List<RegularRow<LearningSymbol<I>>> newRepresentatives,
            MembershipOracle<I, Boolean> membershipOracle) {
        List<RegularRow<LearningSymbol<I>>> newRows = new ArrayList<>(newRepresentatives.size() * alphabet.size());

        for (RegularRow<LearningSymbol<I>> row : newRepresentatives) {
            assert allRows.containsKey(row.getLabel());
            if (!row.isRepresentative()) {
                row.setRepresentative(alphabet.size());
                successorsRows.remove(row);
                representativeRows.add(row);

                Word<LearningSymbol<I>> rep = row.getLabel();
                for (int i = 0; i < alphabet.size(); i++) {
                    Word<LearningSymbol<I>> succ = buildSuccessorWord(rep, alphabet.getSymbol(i));
                    RegularRow<LearningSymbol<I>> succRow = allRows.get(succ);
                    if (succRow == null) {
                        succRow = createSuccessorRow(succ);
                        successorsRows.add(succRow);
                        newRows.add(succRow);
                    }
                    row.addSuccessor(succRow);
                }
            }
        }

        return createNewRows(newRows, membershipOracle);
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> addAlphabetSymbols(
            List<LearningSymbol<I>> symbolsToAdd,
            MembershipOracle<I, Boolean> membershipOracle) {
        alphabet.addAll(symbolsToAdd);

        int numNewRows = representativeRows.size();
        int newSymbolsSize = symbolsToAdd.size();

        List<RegularRow<LearningSymbol<I>>> newRows = new ArrayList<>(numNewRows);

        for (RegularRow<LearningSymbol<I>> repRow : representativeRows) {
            Word<LearningSymbol<I>> rep = repRow.getLabel();
            for (int i = 0; i < newSymbolsSize; i++) {
                Word<LearningSymbol<I>> succ = buildSuccessorWord(rep, symbolsToAdd.get(i));
                RegularRow<LearningSymbol<I>> succRow = allRows.get(succ);
                if (succRow == null) {
                    succRow = createSuccessorRow(succ);
                    successorsRows.add(succRow);
                    newRows.add(succRow);
                }
                repRow.addSuccessor(succRow);
            }
        }

//        alphabet.addAll(symbolsToAdd);
        return createNewRows(newRows, membershipOracle);
    }


    @Override
    public boolean processContents(RegularRow<LearningSymbol<I>> row, List<BitSet> rowContents) {
        int contentId;
        contentId = rowContentsIds.getOrDefault(rowContents, -1);
        if (contentId == -1) {
            contentId = this.rowContents.size();
            this.rowContents.add(rowContents);
            List<BitSet> cloneContent = cloneRowContent(rowContents);
            rowContentsIds.put(cloneContent, contentId);
            row.setContent(contentId);
            if (!primeContextIds.containsKey(cloneContent.getFirst())) {
                primeContextIds.put(cloneContent.getFirst(), primeContext.size());
                primeContext.add(cloneContent.getFirst());
                proceduralSymbolsWord.add(row.getLabel());
                newProceduralSymbols.add(row.getLabel());
            }
            return true;
        }
        row.setContent(contentId);
        return false;
    }

//    public Alphabet<LearningSymbol<I>> getInputAlphabet() {
//        return alphabet;
//    }
//
//    public RegularRow<LearningSymbol<I>> getRepresentative(Word<LearningSymbol<I>> word) {
//        RegularRow<LearningSymbol<I>> row = allRows.get(word);
//        assert row.isRepresentative();
//        return row;
//    }

//    public Collection<RegularRow<LearningSymbol<I>>> getAllRows() {
//        return allRows.values();
//    }

    @Override
    public String toString() {
//        StringBuilder output = new StringBuilder("#cont: \t" + contexts.size() + "\n#sep: \t" + separators.size() + "\n#rep: \t" + representativeRows.size());
        StringBuilder output = new StringBuilder("context: \t" + contexts + "\nsep: \t" + separators);
        for(RegularRow<LearningSymbol<I>> r: representativeRows) {
            output.append("\n").append(r).append("\t").append(rowContents.get(r.getContentId()));
        }
        output.append("\n----------------------");
        for(RegularRow<LearningSymbol<I>> r: successorsRows) {
            if (successorsRows.size() < 200) {
                output.append("\n").append(r).append("\t").append(rowContents.get(r.getContentId()));
            }
        }
        return output.toString();
    }

    @Override
    public int getNumberSymbols() {
        return primeContext.size();
    }
}
