package umons.ac.be.learnlibObservationTable.VRA;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.learner.NewVRALearner.LearningSymbol;
import umons.ac.be.learnlibObservationTable.AbstractRegularObservationTable;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;

import java.util.*;

public class RecursiveObservationTableSeperate<I> extends AbstractRecursiveObservationTable<I> {
    List<RegularRow<LearningSymbol<I>>> primeRows = new ArrayList<>();
    Set<Integer> primeNonRepresentativeIds = new HashSet<>();
    List<Integer> primeRepresentativeIds = new ArrayList<>();

    List<List<Integer>> primesProjections = new ArrayList<>();

    List<List<Integer>> projectedContentToPrimeId = new ArrayList<>();
    List<List<BitSet>> projectedContent = new ArrayList<>();
    List<Map<BitSet, Integer>> projectedContentIds = new ArrayList<>();

    Word<LearningSymbol<I>> inconsistence = null;

    public RecursiveObservationTableSeperate(Alphabet<LearningSymbol<I>> alphabet,
                                             I callSymbol, I returnSymbol,
                                             int query_batch_size) {
        super(alphabet, callSymbol, returnSymbol, query_batch_size);
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> initialize(List<Word<LearningSymbol<I>>> initialRepresentative,
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
            if (processContents(repRow, rowContents)) {
                primeRepresentativeIds.add(primeRows.size());
                primeRows.add(repRow);
            }
        }

        for (RegularRow<LearningSymbol<I>> succRow : successorsRows) {
            if (succRow.isRepresentative()) {
                continue;
            }

            List<BitSet> rowContents = fetchInitialRowResult(queryIt, numSeparators, numContexts);
            if (processContents(succRow, rowContents)) {
                primeNonRepresentativeIds.add(primeRows.size());
                primeRows.add(succRow);
            }
        }

        primesProjections = new ArrayList<>(primeRows.size());
        for (RegularRow<LearningSymbol<I>> r: primeRows) {
            primesProjections.add(new ArrayList<>());
        }

        // return List.of();
        return recomputeAllProjections();
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> toRepresentative(
            List<RegularRow<LearningSymbol<I>>> newRepresentatives,
            MembershipOracle<I, Boolean> membershipOracle) {
        List<RegularRow<LearningSymbol<I>>> representativesToAdd = new ArrayList<>(newRepresentatives.size());
        for (RegularRow<LearningSymbol<I>> row : newRepresentatives) {
            if (primeNonRepresentativeIds.remove(row.getContentId())) {
                primeRepresentativeIds.add(row.getContentId());
                representativesToAdd.add(row);
            }
        }

        // System.out.println("Adding representatives " + representativesToAdd);
        List<RegularRow<LearningSymbol<I>>> unclosed = super.toRepresentative(representativesToAdd, membershipOracle);
        checkInconsistence(representativesToAdd, alphabet, primeContext);
        return unclosed;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> createNewRows(
            List<RegularRow<LearningSymbol<I>>> newRows,
            MembershipOracle<I, Boolean> membershipOracle) {
        int oldPrimeNumbers = primeRows.size();
        int numSeparators = separators.size();
        int numContexts = contexts.size();

        if (numSeparators == 0 || numContexts == 0) {
            return List.of();
        }

        int numNewRows = newRows.size();
        int numRowBatchSize = QUERY_BATCH_SIZE / numSeparators / numContexts + 1;
        assert numRowBatchSize > 0;

        List<DefaultQuery<I, Boolean>> queries;


        for (int start = 0; start < numNewRows; start += numRowBatchSize) {
            // System.out.println("create new rows" + start);
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
                    primeNonRepresentativeIds.add(primeRows.size());
                    primeRows.add(row);
                }
            }

            queries.clear();
        }

        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();
        unclosed.addAll(computeNewProjections(oldPrimeNumbers));
        unclosed.addAll(computeProjectionForNewSymbols());
        return unclosed;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> addContext(Pair<Word<I>, Word<I>> newContext, MembershipOracle<I, Boolean> membershipOracle) {
        if (!contextsSet.add(newContext)) {
            return List.of();
        }
        int oldNumContext = contexts.size();
        contexts.add(newContext);

        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(allRows.size() * separators.size());

        for (RegularRow<LearningSymbol<I>> row : primeRows) {
            buildQueries(queries, row.getLabel(), separators, newContext);
        }

        membershipOracle.processQueries(queries);
        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();
        int numSeparators = separators.size();
        List<BitSet> newPrimeContext = new ArrayList<>();

        for (RegularRow<LearningSymbol<I>> row : primeRows) {
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

                newPrimeContext.add(cloneContent.getFirst());
                // If the context is new, projections also change for old context
                BitSet oldContext = (BitSet) cloneContent.getFirst().clone();
                oldContext.flip(oldNumContext);
                newPrimeContext.add(oldContext);
            }
        }

        queries.clear();

        int rowBatchSize =  QUERY_BATCH_SIZE / separators.size();
        int numSucc = successorsRows.size();

        for (int start = 0; start < successorsRows.size(); start+= rowBatchSize) {
            // System.out.println("cont " + start);
            int numProcessed = Math.min(numSucc-start, rowBatchSize);
            queries = new ArrayList<>(numProcessed * separators.size());

            for (int i = start; i < start + numProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = successorsRows.get(i);
                if (primeRows.get(row.getContentId()).equals(row)) {
                    continue;
                }
                buildQueries(queries,row.getLabel(), separators, newContext);
            }

            membershipOracle.processQueries(queries);
            queryIt = queries.iterator();

            for (int i = start; i < start + numProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = successorsRows.get(i);
                if (primeRows.get(row.getContentId()).equals(row)) {
                    continue;
                }
                List<BitSet> rowContent = cloneRowContent(rowContents.get(row.getContentId()));

                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    rowContent.get(idxSep).clear(oldNumContext);
                    fetchResults(queryIt, rowContent.get(idxSep), oldNumContext);
                }

                if (processContents(row, rowContent)) {
                    primeNonRepresentativeIds.add(primeRows.size());
                    primeRows.add(row);
                }
            }

            queries.clear();
        }

        List<RegularRow<LearningSymbol<I>>> unclosed = recomputeAllProjections(); // TODO we may only recompute them for the newPrimeContext
        checkInconsistence(representativeRows, alphabet, newPrimeContext);
        return unclosed;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> addSeparator(Word<LearningSymbol<I>> newSeparator, MembershipOracle<I, Boolean> membershipOracle) {
        if (!separatorsSet.add(newSeparator)) {
            return List.of();
        }
        int oldNumSeparators = separators.size();
        separators.add(newSeparator);

        List<DefaultQuery<I, Boolean>> queries = new ArrayList<>(allRows.size());

        for (RegularRow<LearningSymbol<I>> row : primeRows) {
            buildQueries(queries, row.getLabel(), newSeparator, contexts);
        }

        membershipOracle.processQueries(queries);

        Iterator<DefaultQuery<I, Boolean>> queryIt = queries.iterator();
        int numContexts = contexts.size();

        for (RegularRow<LearningSymbol<I>> row : primeRows) {
            List<BitSet> rowContent = rowContents.get(row.getContentId());
            rowContentsIds.remove(rowContent);

            BitSet content = new BitSet(numContexts);
            for (int idxContext = 0; idxContext < numContexts; idxContext++) {
                fetchResults(queryIt, content, idxContext);
            }
            rowContent.add(content);

            rowContentsIds.put(cloneRowContent(rowContent), row.getContentId());
        }

        List<RegularRow<LearningSymbol<I>>> unclosed = recomputeProjectionsFromNewSeparator();
        int oldPrimeNumbers = primeRows.size();


        queries.clear();

        int succNum = successorsRows.size();
        int batchRowNum = QUERY_BATCH_SIZE / numContexts;

        for (int start = 0; start < succNum; start += batchRowNum) {
            int numProcessed = Math.min(succNum - start, batchRowNum);
            queries = new ArrayList<>(numProcessed * numContexts);


            for (int i = start; i < start + numProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = successorsRows.get(i);
                if (primeRows.get(row.getContentId()).equals(row)) {
                    continue;
                }
                buildQueries(queries, row.getLabel(), newSeparator, contexts);
            }


            membershipOracle.processQueries(queries);
            queryIt = queries.iterator();

            for (int i = start; i < start + numProcessed; i++) {
                RegularRow<LearningSymbol<I>> row = successorsRows.get(i);
                if (primeRows.get(row.getContentId()).equals(row)) {
                    continue;
                }

                BitSet content = new BitSet(numContexts);
                for (int idxContext = 0; idxContext < numContexts; idxContext++) {
                    fetchResults(queryIt, content, idxContext);
                }

                List<BitSet> parentRowContent = rowContents.get(row.getContentId());
                if (!parentRowContent.getLast().equals(content)) {
                    List<BitSet> newRowContent = cloneRowContent(parentRowContent);
                    newRowContent.set(oldNumSeparators, content);
                    if (processContents(row, newRowContent)) {
                        primeNonRepresentativeIds.add(primeRows.size());
                        primeRows.add(row);
                    }
                }
            }
        }

        unclosed.addAll(computeNewProjections(oldPrimeNumbers));
        unclosed.addAll(computeProjectionForNewSymbols());
        checkInconsistence(representativeRows, alphabet, primeContext);
        return unclosed;
    }

    @Override
    public List<RegularRow<LearningSymbol<I>>> addAlphabetSymbols(
            List<LearningSymbol<I>> symbolsToAdd,
            MembershipOracle<I, Boolean> membershipOracle) {
        List<RegularRow<LearningSymbol<I>>> unclosed = super.addAlphabetSymbols(symbolsToAdd, membershipOracle);
        checkInconsistence(representativeRows, Alphabets.fromCollection(symbolsToAdd), primeContext);
        return unclosed;
    }

    List<RegularRow<LearningSymbol<I>>> recomputeAllProjections() {
        int numSeparators = separators.size();
        int numPrimeContexts = primeContext.size();

        primesProjections = new ArrayList<>(primeRows.size());
        for (RegularRow<LearningSymbol<I>> r: primeRows) {
            primesProjections.add(new ArrayList<>(numPrimeContexts));
        }

        projectedContentToPrimeId = new ArrayList<>(primeContext.size());
        projectedContent = new ArrayList<>(primeContext.size());
        projectedContentIds = new ArrayList<>(primeContext.size());

        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();

        for (int idxContext = 0; idxContext < numPrimeContexts; idxContext++) {
            BitSet b = primeContext.get(idxContext);

            projectedContentToPrimeId.add(new ArrayList<>());
            projectedContent.add(new ArrayList<>());
            projectedContentIds.add(new HashMap<>());

            for (int idxPrime : primeRepresentativeIds) {
                BitSet projection = new BitSet(numSeparators);
                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                        projection.set(idxSep);
                    }
                }
                int contentId = projectedContentIds.getLast().getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.getLast().size();
                    projectedContentIds.getLast().put(projection, contentId);
                    projectedContent.getLast().add(projection);
                    projectedContentToPrimeId.getLast().add(idxPrime);
                }
                primesProjections.get(idxPrime).add(contentId);
            }

            for (int idxPrime : primeNonRepresentativeIds) {
                BitSet projection = new BitSet(numSeparators);
                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                        projection.set(idxSep);
                    }
                }
                int contentId = projectedContentIds.getLast().getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.getLast().size();
                    projectedContentIds.getLast().put(projection, contentId);
                    projectedContent.getLast().add(projection);
                    projectedContentToPrimeId.getLast().add(idxPrime);
                    unclosed.add(primeRows.get(idxPrime));
                }
                primesProjections.get(idxPrime).add(contentId);
            }
        }

        return unclosed;
    }

    List<RegularRow<LearningSymbol<I>>> computeNewProjections(int fromPrimeIdx) {
        if (fromPrimeIdx == primeRows.size()) {
            return List.of();
        }

        int numSeparators = separators.size();
        int numPrimeContexts = projectedContentToPrimeId.size();

        for (int i = fromPrimeIdx; i < primeRows.size(); i++) {
            primesProjections.add(new ArrayList<>(numPrimeContexts));
        }

        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();

        for (int idxContext  = 0; idxContext < numPrimeContexts; idxContext++) {
            BitSet b = primeContext.get(idxContext);
            for (int idxPrime = fromPrimeIdx;  idxPrime < primeRows.size(); idxPrime++) {
                BitSet projection = new BitSet(numSeparators);
                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                        projection.set(idxSep);
                    }
                }

                int contentId = projectedContentIds.get(idxContext).getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.get(idxContext).size();
                    projectedContentIds.get(idxContext).put(projection, contentId);
                    projectedContent.get(idxContext).add(projection);
                    projectedContentToPrimeId.get(idxContext).add(idxPrime);
                    unclosed.add(primeRows.get(idxPrime));
                }
                primesProjections.get(idxPrime).add(contentId);
            }
        }
        return unclosed;
    }

    List<RegularRow<LearningSymbol<I>>> recomputeProjectionsFromNewSeparator() {
        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();
        int idxSep = separators.size()-1;

        for (int idxContext = 0; idxContext < primeContext.size(); idxContext++) {
            BitSet b = primeContext.get(idxContext);
            HashSet<Integer> done = new HashSet<>(projectedContentToPrimeId.get(idxContext).size());
            for (int idxProjPrime = 0; idxProjPrime < projectedContent.get(idxContext).size(); idxProjPrime++) {
                int idxPrime = projectedContentToPrimeId.get(idxContext).get(idxProjPrime);
                done.add(idxPrime);

                BitSet projection = projectedContent.get(idxContext).get(idxProjPrime);
                projectedContentIds.get(idxContext).remove(projection);

                if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                    projection.set(idxSep);
                }

                projectedContentIds.get(idxContext).put(projection, idxProjPrime);
            }

            for (int idxPrime: primeRepresentativeIds) {
                if (done.contains(idxPrime)) {
                    continue;
                }

                BitSet projection = (BitSet) projectedContent.get(idxContext).get(
                        primesProjections.get(idxPrime).get(idxContext)).clone();

                projection.clear(idxSep);

                if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                    projection.set(idxSep);
                }

                int contentId = projectedContentIds.get(idxContext).getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.get(idxContext).size();
                    projectedContentIds.get(idxContext).put(projection, contentId);
                    projectedContent.get(idxContext).add(projection);
                    projectedContentToPrimeId.get(idxContext).add(idxPrime);

                    if (primeNonRepresentativeIds.contains(idxPrime)) {
                        unclosed.add(primeRows.get(idxPrime));
                    }
                }
                primesProjections.get(idxPrime).set(idxContext, contentId);
            }

            for (int idxPrime: primeNonRepresentativeIds) {
                BitSet projection = (BitSet) projectedContent.get(idxContext).get(
                        primesProjections.get(idxPrime).get(idxContext)).clone();

                projection.clear(idxSep);

                if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                    projection.set(idxSep);
                }

                int contentId = projectedContentIds.get(idxContext).getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.get(idxContext).size();
                    projectedContentIds.get(idxContext).put(projection, contentId);
                    projectedContent.get(idxContext).add(projection);
                    projectedContentToPrimeId.get(idxContext).add(idxPrime);

                    if (primeNonRepresentativeIds.contains(idxPrime)) {
                        unclosed.add(primeRows.get(idxPrime));
                    }
                }
                primesProjections.get(idxPrime).set(idxContext, contentId);
            }
        }
        return unclosed;
    }

    List<RegularRow<LearningSymbol<I>>> computeProjectionForNewSymbols() {
        if (newProceduralSymbols.isEmpty()) {
            return List.of();
        }

        int numSeparators = separators.size();
        int oldNumPrimeContext = projectedContentToPrimeId.size();
        int numPrimeContexts = primeContext.size();


        if(oldNumPrimeContext == numPrimeContexts) {
            return List.of();
        }

        List<RegularRow<LearningSymbol<I>>> unclosed = new ArrayList<>();

        for (int idxContext = oldNumPrimeContext; idxContext < numPrimeContexts; idxContext++) {
            BitSet b = primeContext.get(idxContext);

            projectedContentToPrimeId.add(new ArrayList<>());
            projectedContent.add(new ArrayList<>());
            projectedContentIds.add(new HashMap<>());

            for (int idxPrime : primeRepresentativeIds) {
                BitSet projection = new BitSet(numSeparators);
                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                        projection.set(idxSep);
                    }
                }
                int contentId = projectedContentIds.getLast().getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.getLast().size();
                    projectedContentIds.getLast().put(projection, contentId);
                    projectedContent.getLast().add(projection);
                    projectedContentToPrimeId.getLast().add(idxPrime);
                }
                primesProjections.get(idxPrime).add(contentId);
            }

            for (int idxPrime : primeNonRepresentativeIds) {
                BitSet projection = new BitSet(numSeparators);
                for (int idxSep = 0; idxSep < numSeparators; idxSep++) {
                    if (rowContents.get(idxPrime).get(idxSep).equals(b)) {
                        projection.set(idxSep);
                    }
                }
                int contentId = projectedContentIds.getLast().getOrDefault(projection, -1);
                if (contentId == -1) {
                    contentId = projectedContentIds.getLast().size();
                    projectedContentIds.getLast().put(projection, contentId);
                    projectedContent.getLast().add(projection);
                    projectedContentToPrimeId.getLast().add(idxPrime);
                    unclosed.add(primeRows.get(idxPrime));
                }
                primesProjections.get(idxPrime).add(contentId);
            }
            // System.out.println(primeRepresentativeIds + " " + primeNonRepresentativeIds);
        }

        return unclosed;
    }

    public List<RegularRow<LearningSymbol<I>>> createNewProceduralSymbol(Word<LearningSymbol<I>> word, BitSet T_val) {
        BitSet cloneT_val = (BitSet) T_val.clone();
        primeContextIds.put(cloneT_val, primeContext.size());
        primeContext.add(cloneT_val);
        proceduralSymbolsWord.add(word);
        newProceduralSymbols.add(word);
        List<RegularRow<LearningSymbol<I>>> unclosed = computeProjectionForNewSymbols();

        // System.out.println("Adding temporary symbol " + word);
        // System.out.println(this);
        // System.out.println(unclosed);
        newProceduralSymbols.removeLast();
        return unclosed;
    }

    public Word<LearningSymbol<I>> removeProceduralSymbol(Word<LearningSymbol<I>> word, BitSet T) {
        assert primeContextIds.containsKey(T);
        assert proceduralSymbolsWord.get(primeContextIds.get(T)).equals(word);

        if (allRows.containsKey(word) && representativeRows.contains(allRows.get(word))) {
            newProceduralSymbols.add(word);
            return word;
        }

        for (RegularRow<LearningSymbol<I>> row: representativeRows) {
            if (rowContents.get(row.getContentId()).getFirst().equals(T)) {
                proceduralSymbolsWord.set(
                        primeContextIds.get(T), row.getLabel()
                );
                newProceduralSymbols.add(row.getLabel());
                return row.getLabel();
            }
        }
        assert false;
        return null;
    }

    public int getContextId(BitSet b) {
        return primeContextIds.get(b);
    }

    public List<RegularRow<LearningSymbol<I>>> getRepresentativeRows(int contextId) {
        List<RegularRow<LearningSymbol<I>>> result = new ArrayList<>();
        for (RegularRow<LearningSymbol<I>> row: representativeRows) {
            // System.out.println(projectedContentToPrimeId + " " + projectedContentIds);
            if (projectedContentToPrimeId.get(contextId).get(
                    primesProjections.get(row.getContentId()).get(contextId)
                ).equals(row.getContentId())) {
                result.add(row);
            }
        }
        return result;
    }

    public RegularRow<LearningSymbol<I>> getSuccessor(
            RegularRow<LearningSymbol<I>> row,
            LearningSymbol<I> input,
            int contextId) {
        RegularRow<LearningSymbol<I>> succ = row.getSuccessor(alphabet.getSymbolIndex(input));
        // System.out.println(this);
        // System.out.println("primeRows: " + primeRows);
        // System.out.println("projectedContentToPrimeId: " + projectedContentToPrimeId);
        // System.out.println("primesProjections: " + primesProjections);
        // System.out.println("succ: " + succ);
        // System.out.println("contextId: " + contextId);
        return primeRows.get(
                projectedContentToPrimeId.get(contextId).get(
                        primesProjections.get(succ.getContentId()).get(contextId)
                )
        );
    }

    public Word<LearningSymbol<I>> getRecursiveEquivalent(BitSet b) {
        if (!primeContextIds.containsKey(b)) {
            return null;
        }
        return proceduralSymbolsWord.get(primeContextIds.get(b));
    }

    public Word<LearningSymbol<I>> getEquivalentRep(Word<LearningSymbol<I>> prefix, BitSet T) {
        assert primeContextIds.containsKey(T);
        int contextId =  primeContextIds.get(T);

        // if (allRows.containsKey(prefix)) {
        //    return primeRows.get(
        //            projectedContentToPrimeId.get(contextId).get(
        //                    primesProjections.get(allRows.get(prefix).getContentId()).get(contextId)
        //            )
        //    ).getLabel();
        // }

        RegularRow<LearningSymbol<I>> row = allRows.get(Word.epsilon());
        for (LearningSymbol<I> input: prefix) {
            row = getSuccessor(row, input, contextId);
            // System.out.println(input + " " + row);
            // System.out.println(this);
            assert representativeRows.contains(row);
        }
        return row.getLabel();
    }

    public List<BitSet> getPrimeContexts() {
        return primeContext;
    }

    public boolean isAccepted(Word<LearningSymbol<I>> word, BitSet T) {
        return rowContents.get(
                allRows.get(getEquivalentRep(word, T)).getContentId()
        ).getFirst().equals(T);
    }

    public int getNumberStates(){
        int numberStates = 0;
        for (List<BitSet> l: projectedContent) {
            numberStates += l.size();
        }
        return numberStates;
    }

    public void checkInconsistence(
            List<RegularRow<LearningSymbol<I>>> representativesToCheck,
            Alphabet<LearningSymbol<I>> symbolsToCheck,
            List<BitSet> contextsToCheck
    ) {
        if (inconsistence != null) {
            return;
        }
        for (BitSet context : contextsToCheck) {
            int contextId = primeContextIds.get(context);
            for (RegularRow<LearningSymbol<I>> row: representativesToCheck) {
                int rowId = row.getContentId();
                int rowProjId = primesProjections.get(rowId).get(contextId);

                // skip if the row is equivalent to itself
                if (projectedContentToPrimeId.get(contextId).get(rowProjId).equals(rowId)) {
                    continue;
                }

                RegularRow<LearningSymbol<I>> equivalentRow =
                        primeRows.get(projectedContentToPrimeId.get(contextId).get(rowProjId));

                assert row.isRepresentative();
                assert equivalentRow.isRepresentative();

                // Check inconsistence for each symbols
                for (LearningSymbol<I> input: symbolsToCheck) {
                    int symbolId = alphabet.getSymbolIndex(input);

                    // Get IDs of projected successors
                    int succProjId = primesProjections.get(
                            row.getSuccessor(symbolId).getContentId()).get(contextId);
                    int equivalentSuccProjId = primesProjections.get(
                            equivalentRow.getSuccessor(symbolId).getContentId()).get(contextId);

                    // Check if projected successors are the same
                    if (equivalentSuccProjId != succProjId) {
                        // If not, retrieve the inconsistence
                        BitSet diff = (BitSet) projectedContent.get(contextId).get(equivalentSuccProjId).clone();
                        diff.xor(projectedContent.get(contextId).get(succProjId));

                        int firstDifferentIndex = diff.nextSetBit(0);
                        assert firstDifferentIndex != -1;

                        inconsistence = Word.fromWords(Word.fromLetter(input), separators.get(firstDifferentIndex));
                        return; // One inconsistence at a time, no need to continue after finding one
                    }
                }
            }
        }
    }

    public Word<LearningSymbol<I>> getInconsistence() {
        if (inconsistence == null) {
            return null;
        }
        Word<LearningSymbol<I>> separatorToAdd = Word.fromWords(inconsistence);
        inconsistence = null;
        return separatorToAdd;
    }
}
