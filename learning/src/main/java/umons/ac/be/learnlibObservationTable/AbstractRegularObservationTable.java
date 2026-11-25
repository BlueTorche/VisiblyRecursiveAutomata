package umons.ac.be.learnlibObservationTable;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.word.Word;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.*;

public abstract class AbstractRegularObservationTable<I, A, R, S> {
    private final List<S> separators = new ArrayList<>();
    private final Set<S> separatorsSet = new HashSet<>();

    private final List<RegularRow<R>> representativeRows = new ArrayList<>();
    private final List<RegularRow<R>> successorsRows = new ArrayList<>();
    private final Map<Word<R>, RegularRow<R>> allRows = new HashMap<>();

    private final List<BitSet> rowContent = new ArrayList<>();
    private final Map<BitSet, Integer> rowContentIds = new HashMap<>();

    private final Alphabet<A> alphabet;

    protected final int QUERY_BATCH_SIZE;

    protected AbstractRegularObservationTable(Alphabet<A> alphabet, int query_batch_size) {
        this.alphabet = new GrowingMapAlphabet<>(alphabet);
        QUERY_BATCH_SIZE = query_batch_size;
    }

    protected abstract void buildQueries(List<DefaultQuery<I, Boolean>> queryList,
                                         Word<R> representative,
                                         List<S> separators);

    private void buildQueries(List<DefaultQuery<I, Boolean>> queryList,
                              Word<R> representative,
                              S separator) {
        buildQueries(queryList, representative, Collections.singletonList(separator));
    }


    public abstract List<RegularRow<R>> initialize(MembershipOracle<I, Boolean> membershipOracle);

    public List<RegularRow<R>> initializeWithSeparators(
            List<Word<R>> initialRepresentative,
            List<S> initialSeparators,
            MembershipOracle<I, Boolean> membershipOracle) {
        if (!allRows.isEmpty()) {
            throw new IllegalStateException("Called initialize, but there are already rows present");
        }

        if (!checkPrefixClosed(initialRepresentative)) {
            throw new IllegalArgumentException("Initial short prefixes are not prefix-closed");
        }

        int numSeparators = initialSeparators.size();
        for (S c : initialSeparators) {
            if (separatorsSet.add(c)) {
                separators.add(c);
            }
        }

        int numRows = alphabet.size() * initialRepresentative.size() + 1;
        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(numSeparators * numRows);

        // PASS 1: Add short prefix rows
        for (Word<R> r : initialRepresentative) {
            representativeRows.add(createRepresentativeRow(r));
            buildQueries(queries, r, separators);
        }

        // PASS 2: Add missing long prefix rows
        for (RegularRow<R> repRow : representativeRows) {
            Word<R> rep = repRow.getLabel();
            for (int i = 0; i < alphabet.size(); i++) {
                Word<R> succ = buildSuccessorWord(rep, alphabet.getSymbol(i));
                RegularRow<R> succRow = allRows.get(succ);
                if (succRow == null) {
                    succRow = createSuccessorRow(succ);
                    successorsRows.add(succRow);
                    buildQueries(queries, succ, separators);
                }
                repRow.addSuccessor(succRow);
            }
        }

        membershipOracle.processQueries(queries);
        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();

        for (RegularRow<R> repRow : representativeRows) {
            BitSet rowContents = new BitSet(numSeparators);
            for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                fetchResults(queryIt, rowContents, idxSep);
            }
            processContents(repRow, rowContents);
        }

        List<RegularRow<R>> unclosed = new ArrayList<>();

        for (RegularRow<R> succRow : successorsRows) {
            if (succRow.isRepresentative()) {
                continue;
            }
            BitSet rowContents = new BitSet(numSeparators);
            for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                fetchResults(queryIt, rowContents, idxSep);
            }

            if (processContents(succRow, rowContents)) {
                unclosed.add(succRow);
            }
        }

        return unclosed;
    }

    protected abstract Word<R> buildSuccessorWord(Word<R> representative, A symbol);

    public boolean isInitialized() {
        return !allRows.isEmpty();
    }

    public List<RegularRow<R>> addSeparator(
            S newSeparator,
            MembershipOracle<I, Boolean> membershipOracle) {
        if (!separatorsSet.add(newSeparator)) {
            return List.of();
        }
        int oldNumSeparators = separators.size();
        separators.add(newSeparator);


        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(representativeRows.size());

        for (RegularRow<R> row : representativeRows) {
            buildQueries(queries, row.getLabel(), newSeparator);
        }

        membershipOracle.processQueries(queries);

        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();

        for (RegularRow<R> row : representativeRows) {
            BitSet content = rowContent.get(row.getContentId());
            rowContentIds.remove(content);
            fetchResults(queryIt, content, oldNumSeparators);
            rowContentIds.put((BitSet) content.clone(), row.getContentId());
        }

        queries.clear();

        int succNum = successorsRows.size();
        List<RegularRow<R>> unclosed = new ArrayList<>();

        for (int start = 0; start < succNum; start += QUERY_BATCH_SIZE) {
            int numQueries = Math.min(succNum-start, QUERY_BATCH_SIZE);
            queries = new ArrayList<>(numQueries);

            for (int i = start; i < start + numQueries; i++) {
                buildQueries(queries, successorsRows.get(i).getLabel(), newSeparator);
            }

            membershipOracle.processQueries(queries);

            queryIt = queries.iterator();
            for (int i = start; i < start + numQueries; i++) {
                RegularRow<R> row = successorsRows.get(i);
                BitSet content = (BitSet) rowContent.get(row.getContentId()).clone();
                content.clear(oldNumSeparators);
                fetchResults(queryIt, content, oldNumSeparators);
                if (processContents(row, content)) {
                    unclosed.add(row);
                }
            }

            queries.clear();
        }

        return unclosed;
    }

    public List<RegularRow<R>> toRepresentative(
            List<RegularRow<R>> newRepresentatives,
            MembershipOracle<I, Boolean> membershipOracle) {
        List<RegularRow<R>> newRows = new ArrayList<>(newRepresentatives.size() * alphabet.size());

        for (RegularRow<R> row : newRepresentatives) {
            assert allRows.containsKey(row.getLabel());
            if (!row.isRepresentative()) {
                row.setRepresentative(alphabet.size());
                successorsRows.remove(row);
                representativeRows.add(row);

                Word<R> rep = row.getLabel();
                for (int i = 0; i < alphabet.size(); i++) {
                    Word<R> succ = buildSuccessorWord(rep, alphabet.getSymbol(i));
                    RegularRow<R> succRow = allRows.get(succ);
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

    private List<RegularRow<R>> createNewRows(
            List<RegularRow<R>> newRows,
            MembershipOracle<I, Boolean> membershipOracle) {
        int numRows = newRows.size();
        int numSeparators = separators.size();

        int numRowsBatch =  QUERY_BATCH_SIZE / numSeparators + 1;

        List<RegularRow<R>> unclosed = new ArrayList<>();

        for (int start = 0; start < numRows; start += numRowsBatch) {
            int numRowsProcessed = Math.min(numRows-start, numRowsBatch);
            List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(numRowsProcessed * numSeparators);
            for (int i = start; i < start + numRowsProcessed; i++) {
                buildQueries(queries, newRows.get(i).getLabel(), separators);
            }

            membershipOracle.processQueries(queries);
            Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();

            for (int i = start; i < start + numRowsProcessed; i++) {
                RegularRow<R> newRow = newRows.get(i);
                BitSet rowContents = new BitSet(numSeparators);
                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    fetchResults(queryIt, rowContents, idxSep);
                }
                if (processContents(newRow, rowContents)) {
                    unclosed.add(newRow);
                }
            }

            queries.clear();
        }

        return unclosed;
    }

    public List<RegularRow<R>> addAlphabetSymbols(
            List<A> newSymbols,
            MembershipOracle<I, Boolean> membershipOracle) {
        List<A> symbolsToAdd = new ArrayList<>(newSymbols.size());
        for (A symbol : newSymbols) {
            if (alphabet.add(symbol)) {
                symbolsToAdd.add(symbol);
            }
        }

        int numNewRows = representativeRows.size();
        int newSymbolsSize = symbolsToAdd.size();

        List<RegularRow<R>> newRows = new ArrayList<>(numNewRows);

        for (RegularRow<R> repRow : representativeRows) {
            Word<R> rep = repRow.getLabel();
            for (int i = 0; i < newSymbolsSize; i++) {
                Word<R> succ = buildSuccessorWord(rep, symbolsToAdd.get(i));
                RegularRow<R> succRow = allRows.get(succ);
                if (succRow == null) {
                    succRow = createSuccessorRow(succ);
                    successorsRows.add(succRow);
                    newRows.add(succRow);
                }
                repRow.addSuccessor(succRow);
            }
        }

        alphabet.addAll(symbolsToAdd);
        return createNewRows(newRows, membershipOracle);
    }

    private static <R> boolean checkPrefixClosed(Collection<? extends Word<R>> initialShortPrefixes) {
        Set<Word<R>> prefixes = new HashSet<>(initialShortPrefixes);

        for (Word<R> pref : initialShortPrefixes) {
            if (!pref.isEmpty() && !prefixes.contains(pref.prefix(-1))) {
                return false;
            }
        }

        return true;
    }

    private RegularRow<R> createRepresentativeRow(Word<R> prefix) {
        if (allRows.containsKey(prefix)) {
            return allRows.get(prefix);
        }
        RegularRow<R> newRow = new RegularRow<>(prefix, alphabet.size());
        allRows.put(prefix, newRow);
        return newRow;
    }

    private RegularRow<R> createSuccessorRow(Word<R> prefix) {
        if (allRows.containsKey(prefix)) {
            return allRows.get(prefix);
        }
        RegularRow<R> newRow = new RegularRow<>(prefix);
        allRows.put(prefix, newRow);
        return newRow;
    }


    /**
     * Fetches the given number of query responses and adds them to the specified output list. Also, the query iterator
     * is advanced accordingly.
     *
     * @param queryIt
     *         the query iterator
     * @param output
     *         the output list to write to
     * @param idxSep
     *         the index of the separator queried
     */
    private static <I> void fetchResults(
            Iterator<DefaultQuery<I, Boolean>> queryIt,
            BitSet output,
            int idxSep) {
        if (queryIt.next().getOutput()) {
            output.set(idxSep);
        }
    }

    private boolean processContents(RegularRow<R> row, BitSet content) {
        int contentId;
        contentId = rowContentIds.getOrDefault(content, -1);
        if (contentId == -1) {
            contentId = rowContent.size();
            rowContent.add(content);
            rowContentIds.put((BitSet) content.clone(), contentId);
            row.setContent(contentId);
            return true;
        }
        row.setContent(contentId);
        return false;
    }

    public Alphabet<A> getInputAlphabet() {
        return alphabet;
    }

    public List<RegularRow<R>> getRepresentativeRows() {
        return representativeRows;
    }

    public RegularRow<R> getRepresentative(Word<R> word) {
        RegularRow<R> row = allRows.get(word);
        assert row.isRepresentative();
        return row;
    }

    public Collection<RegularRow<R>> getAllRows() {
        return allRows.values();
    }

    public List<S> getSeparators() {
        return separators;
    }

    public boolean isFinal(RegularRow<R> row) {
        return rowContent.get(row.getContentId()).get(0);
    }

    public Word<R> getSuccessor(Word<R> currentState, A input) {
        RegularRow<R> row = allRows.get(currentState);
        assert row != null;
        assert row.isRepresentative();
        return getSuccessor(row, input).getLabel();
    }

    public RegularRow<R> getSuccessor(RegularRow<R> row, A input) {
        int succIdx = alphabet.getSymbolIndex(input);
        assert succIdx >= 0;
        return representativeRows.get(row.getSuccessor(succIdx).getContentId());
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("#sep:\t" + separators.size() + "\n#rep:\t" + representativeRows.size());
//        StringBuilder output = new StringBuilder("\t  " + separators);
//        for(RegularRow<R> r: representativeRows) {
//            output.append("\n").append(r).append("\t").append(rowContent.get(r.getContentId()));
//        }
//        output.append("\n----------------------");
//        for(RegularRow<R> r: successorsRows) {
//            if (!rowContent.get(r.getContentId()).isEmpty()) {
//                output.append("\n").append(r).append("\t").append(rowContent.get(r.getContentId()));
//            }
//        }
        return output.toString();
    }
}
