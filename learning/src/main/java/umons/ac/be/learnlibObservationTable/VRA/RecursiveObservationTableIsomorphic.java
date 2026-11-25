package umons.ac.be.learnlibObservationTable.VRA;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.learner.NewVRALearner.LearningSymbol;
import umons.ac.be.learnlibObservationTable.AbstractRegularObservationTable;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.*;

public class RecursiveObservationTableIsomorphic<I> extends AbstractRecursiveObservationTable<I> {
    public RecursiveObservationTableIsomorphic(Alphabet<LearningSymbol<I>> alphabet,
                                               I callSymbol, I returnSymbol,
                                               int query_batch_size) {
        super(alphabet, callSymbol, returnSymbol, query_batch_size);
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> initialize(
            List<Word<LearningSymbol<I>>> initialRepresentative,
            List<Word<LearningSymbol<I>>> initialSeparators,
            List<Pair<Word<I>, Word<I>>> initialContexts,
            MembershipOracle<I, Boolean> membershipOracle) {
        if (!allRows.isEmpty()) {
            throw new IllegalStateException("Called initialize, but there are already rows present");
        }

        if (!checkPrefixClosed(initialRepresentative)) {
            throw new IllegalArgumentException("Initial short prefixes are not prefix-closed");
        }

        int numSeparators = initialSeparators.size();
        for (Word<LearningSymbol<I>> c : initialSeparators) {
            if (separatorsSet.add(c)) {
                separators.add(c);
            }
        }

        int numContexts = initialContexts.size();
        for (Pair<Word<I>, Word<I>> c : initialContexts) {
            if (contextsSet.add(c)) {
                contexts.add(c);
            }
        }

        int numRows = alphabet.size() * initialRepresentative.size() + 1;
        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(numSeparators * numRows * numContexts);

        // PASS 1: Add short prefix rows
        for (Word<LearningSymbol<I>> r : initialRepresentative) {
            representativeRows.add(createRepresentativeRow(r));
            buildQueries(queries, r, separators, contexts);
        }

        // PASS 2: Add missing long prefix rows
        for (RegularRow<LearningSymbol<I>> repRow : representativeRows) {
            Word<LearningSymbol<I>> rep = repRow.getLabel();
            for (int i = 0; i < alphabet.size(); i++) {
                Word<LearningSymbol<I>> succ = buildSuccessorWord(rep, alphabet.getSymbol(i));
                RegularRow<LearningSymbol<I>> succRow = allRows.get(succ);
                if (succRow == null) {
                    succRow = createSuccessorRow(succ);
                    successorsRows.add(succRow);
                    buildQueries(queries, succ, separators, contexts);
                }
                repRow.addSuccessor(succRow);
            }
        }

        membershipOracle.processQueries(queries);
        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();

        for (RegularRow<LearningSymbol<I>> repRow : representativeRows) {
            List<BitSet> rowContents = fetchInitialRowResult(queryIt, numSeparators, numContexts);
            processContents(repRow, rowContents);
        }

        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();

        for (RegularRow<LearningSymbol<I>> succRow : successorsRows) {
            if (succRow.isRepresentative()) {
                continue;
            }

            List<BitSet> rowContents = fetchInitialRowResult(queryIt, numSeparators, numContexts);
            if (processContents(succRow, rowContents)) {
                unclosed.add(succRow);
            }
        }

        return unclosed;
    }


    @Override
    public List<RegularRow<LearningSymbol<I>>> addSeparator(
            Word<LearningSymbol<I>> newSeparator,
            MembershipOracle<I, Boolean> membershipOracle) {
        if (!separatorsSet.add(newSeparator)) {
            return List.of();
        }
        int oldNumSeparators = separators.size();
        separators.add(newSeparator);

        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(representativeRows.size());

        for (RegularRow<LearningSymbol<I>> row : representativeRows) {
            buildQueries(queries, row.getLabel(), newSeparator, contexts);
        }

        membershipOracle.processQueries(queries);

        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();
        int numContexts = contexts.size();

        for (RegularRow<LearningSymbol<I>> row : representativeRows) {
            List<BitSet> rowContent = rowContents.get(row.getContentId());
            rowContentsIds.remove(rowContent);

            BitSet content = new BitSet(numContexts);
            for (int idxContext = 0; idxContext < numContexts; idxContext++) {
                fetchResults(queryIt, content, idxContext);
            }
            rowContent.add(content);

            rowContentsIds.put(cloneRowContent(rowContent), row.getContentId());
        }

        queries.clear();

        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();
        int succNum = successorsRows.size();
        int batchRowNum = QUERY_BATCH_SIZE / numContexts + 1;

        for (int start = 0; start < succNum; start += batchRowNum) {
            int numProcessed = Math.min(succNum-start, batchRowNum);
            queries = new ArrayList<>(numProcessed * numContexts);

            for (int i = start; i < start + numProcessed; i++) {
                buildQueries(queries, successorsRows.get(i).getLabel(), newSeparator, contexts);
            }

            membershipOracle.processQueries(queries);
            queryIt = queries.iterator();


            for (int i = start; i < start + numProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = successorsRows.get(i);
                BitSet content = new BitSet(numContexts);
                for (int idxContext = 0; idxContext < numContexts; idxContext++) {
                    fetchResults(queryIt, content, idxContext);
                }

                List<BitSet> parentRowContent = rowContents.get(row.getContentId());
                if (!parentRowContent.getLast().equals(content)) {
                    List<BitSet> newRowContent = cloneRowContent(parentRowContent);
                    newRowContent.set(oldNumSeparators, content);
                    if (processContents(row, newRowContent)) {
                        unclosed.add(row);
                    }
                }
            }

            queries.clear();
        }

        return unclosed;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> addContext(
            Pair<Word<I>, Word<I>> newContext,
            MembershipOracle<I, Boolean> membershipOracle) {
        if (!contextsSet.add(newContext)) {
            return List.of();
        }
        int oldNumContext = contexts.size();
        contexts.add(newContext);

        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(allRows.size() * separators.size());

        for (RegularRow<LearningSymbol<I>> row : representativeRows) {
            buildQueries(queries, row.getLabel(), separators, newContext);
        }

        membershipOracle.processQueries(queries);

        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();
        int numSeparators = separators.size();

        for (RegularRow<LearningSymbol<I>> row : representativeRows) {
            List<BitSet> rowContent = rowContents.get(row.getContentId());
            rowContentsIds.remove(rowContent);

            int existingProcSymbol = -1;
            if (primeContextIds.containsKey(rowContent.getFirst()) &&
                    proceduralSymbolsWord.get(primeContextIds.get(rowContent.getFirst())).equals(row.getLabel())) {
                existingProcSymbol = primeContextIds.get(rowContent.getFirst());
                primeContextIds.remove(rowContent.getFirst());
            }

            for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                fetchResults(queryIt, rowContent.get(idxSep), oldNumContext);
            }

            List<BitSet> cloneContent = cloneRowContent(rowContent);
            rowContentsIds.put(cloneContent, row.getContentId());

            if (existingProcSymbol >= 0) {
                primeContextIds.put(cloneContent.getFirst(), existingProcSymbol);
                primeContext.set(existingProcSymbol, cloneContent.getFirst());
            }
            if (!primeContextIds.containsKey(rowContent.getFirst())) {
                primeContextIds.put(cloneContent.getFirst(), primeContext.size());
                primeContext.add(cloneContent.getFirst());
                proceduralSymbolsWord.add(row.getLabel());
                newProceduralSymbols.add(row.getLabel());
            }
        }

        queries.clear();

        int rowBatchSize =  QUERY_BATCH_SIZE / separators.size() + 1;
        int numSucc = successorsRows.size();
        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();

        for (int start = 0; start < numSucc; start += rowBatchSize) {
            int numProcessed = Math.min(numSucc-start, rowBatchSize);
            queries = new ArrayList<>(numProcessed * separators.size());

            for (int i = start; i < start + numProcessed; i++) {
                buildQueries(queries, successorsRows.get(i).getLabel(), separators, newContext);
            }

            membershipOracle.processQueries(queries);
            queryIt = queries.iterator();

            for (int i = start; i < start + numProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = successorsRows.get(i);
                List<BitSet> rowContent = cloneRowContent(rowContents.get(row.getContentId()));

                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    rowContent.get(idxSep).clear(oldNumContext);
                    fetchResults(queryIt, rowContent.get(idxSep), oldNumContext);
                }

                if (processContents(row, rowContent)) {
                    unclosed.add(row);
                }

            }

            queries.clear();
        }

        return unclosed;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> createNewRows(
            List<RegularRow<LearningSymbol<I>>> newRows,
            MembershipOracle<I, Boolean> membershipOracle) {
        int numSeparators = separators.size();
        int numContexts = contexts.size();

        if (numSeparators == 0 || numContexts == 0) {
            return List.of();
        }

        int numNewRows = newRows.size();
        int numRowBatchSize = QUERY_BATCH_SIZE / numSeparators / numContexts + 1;
        assert numRowBatchSize > 0;

        List<DefaultQuery<I, Boolean>> queries;
        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();

        for (int start = 0; start < numNewRows; start += numRowBatchSize) {
            int numRowProcessed = Math.min(numNewRows-start, numRowBatchSize);
            queries = new ArrayList<>(numRowProcessed * numSeparators * numContexts);

            for (int i = start; i < start + numRowProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = newRows.get(i);
                assert allRows.containsKey(row.getLabel());
                buildQueries(queries, row.getLabel(), separators, contexts);
            }

            membershipOracle.processQueries(queries);
            Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();

            for (int i = start; i < start + numRowProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = newRows.get(i);
                List<BitSet> rowContents = fetchInitialRowResult(queryIt, numSeparators, numContexts);
                if (processContents(row, rowContents)) {
                    unclosed.add(row);
                }
            }

            queries.clear();
        }

        return unclosed;
    }


    public Word<LearningSymbol<I>> getEquivalentWord(Word<LearningSymbol<I>> word) {
        RegularRow<LearningSymbol<I>> row = representativeRows.getFirst();
        for (LearningSymbol<I> s: word) {
            row = getSuccessor(row, s);
        }
        return proceduralSymbolsWord.get(primeContextIds.get(rowContents.get(row.getContentId()).getFirst()));
    }

    public Word<LearningSymbol<I>> getBitsetEquivalentWord(BitSet bitset) {
        if (!primeContextIds.containsKey(bitset)) {
            return null;
        }
        return proceduralSymbolsWord.get(primeContextIds.get(bitset));
    }

    public Word<LearningSymbol<I>> getEquivalentRep(Word<LearningSymbol<I>> word) {
        RegularRow<LearningSymbol<I>> row = representativeRows.getFirst();
        for (LearningSymbol<I> s: word) {
            row = getSuccessor(row, s);
        }
        return row.getLabel();
    }

    public Word<LearningSymbol<I>> getSuccessor(Word<LearningSymbol<I>> currentState, LearningSymbol<I> input) {
        RegularRow<LearningSymbol<I>> row = allRows.get(currentState);
        assert row != null;
        assert row.isRepresentative();
        return getSuccessor(row, input).getLabel();
    }

    public RegularRow<LearningSymbol<I>> getSuccessor(RegularRow<LearningSymbol<I>> row, LearningSymbol<I> input) {
        int succIdx = alphabet.getSymbolIndex(input);
        assert succIdx >= 0;
        return representativeRows.get(row.getSuccessor(succIdx).getContentId());
    }
}

