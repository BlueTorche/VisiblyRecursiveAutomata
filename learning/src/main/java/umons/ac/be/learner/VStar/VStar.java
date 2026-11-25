package umons.ac.be.learner.VStar;

import de.learnlib.LearnerStateTracker;
import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.automaton.vpa.impl.DefaultNSEVPA;
import net.automatalib.automaton.vpa.impl.Location;
import net.automatalib.common.util.Pair;
import net.automatalib.common.util.Triple;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.learnlibObservationTable.AbstractRegularObservationTable;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;
import umons.ac.be.learnlibObservationTable.VStar.VStarCallObservationTable;
import umons.ac.be.learnlibObservationTable.VStar.VStarInitialObservationTable;

import java.util.*;

public class VStar<I>
        implements LearningAlgorithm<SEVPA<?, I>, I, Boolean>,
                   LearnerStateTracker {
    protected VPAlphabet<I> alphabet;
    protected Alphabet<Word<I>> tableAlphabet = new GrowingMapAlphabet<>();
    protected MembershipOracle<I, Boolean> membershipOracle;
    protected EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle;

    protected VStarInitialObservationTable<I> initialTable;
    protected Map<I, VStarCallObservationTable<I>> callTables = new HashMap<>();

    protected List<RegularRow<I>> initialUnclosed;
    protected Map<I, List<RegularRow<I>>> callUnclosed = new HashMap<>();

    protected VStar(VPAlphabet<I> alphabet, MembershipOracle<I, Boolean> oracle, int query_batch_size) {
        this.alphabet = alphabet;
        this.membershipOracle = oracle;

        for (I i: alphabet.getInternalAlphabet()) {
            tableAlphabet.asGrowingAlphabetOrThrowException().add(Word.fromSymbols(i));
        }
        for (I c: alphabet.getCallAlphabet()) {
            for (I r: alphabet.getReturnAlphabet()) {
                tableAlphabet.asGrowingAlphabetOrThrowException().add(Word.fromSymbols(c, r));
            }
        }

        initialTable = new VStarInitialObservationTable<>(tableAlphabet, query_batch_size);
        for (I c: alphabet.getCallAlphabet()) {
            callTables.put(c, new VStarCallObservationTable<>(tableAlphabet, query_batch_size));
        }
    }

    @Override
    public void startLearning() {
        requireLearningProcessNotStarted();
        initialUnclosed = initialTable.initialize(membershipOracle);
        for (I c: alphabet.getCallAlphabet()) {
            List<Pair<Word<I>, Word<I>>> contexts = new ArrayList<>(alphabet.getNumReturns());
            for (I r: alphabet.getReturnAlphabet()) {
                contexts.add(Pair.of(Word.fromSymbols(c), Word.fromSymbols(r)));
            }
            callUnclosed.put(c, callTables.get(c).initializeWithSeparators(
                    Collections.singletonList(Word.epsilon()),
                    contexts,
                    membershipOracle
            ));
        }

//        printTables();
        enforceTables();
    }

    protected void enforceTables() {
        initialUnclosed = initialTable.toRepresentative(initialUnclosed, membershipOracle);

        List<Word<I>> wordToAdd = new ArrayList<>();
        for (I c: alphabet.getCallAlphabet()) {
            for (RegularRow<I> row: callUnclosed.get(c)) {
                Word<I> w = row.getLabel();
                for (I r : alphabet.getReturnAlphabet()) {
                    wordToAdd.add(Word.fromWords(Word.fromLetter(c), w, Word.fromLetter(r)));
                }
            }
            callUnclosed.put(c, callTables.get(c).toRepresentative(callUnclosed.get(c), membershipOracle));
        }
        addWordSymbols(wordToAdd);

        if (!initialUnclosed.isEmpty() || !callUnclosed.values().stream().allMatch(List::isEmpty)) {
            enforceTables();
        }
    }

    @Override
    public final boolean refineHypothesis(DefaultQuery<I, Boolean> ceQuery) {
        requireLearningProcessStarted();

//        if (!MQUtil.isCounterexample(ceQuery, hypothesisOutput())) {
//            return false;
//        }

//        printTables();
        Word<I> cx = ceQuery.getInput();
        boolean positiveCX = ceQuery.getOutput();

        while (true) {
            Triple<I, Word<I>, Word<I>> separator = getSeparator(cx, positiveCX);
            if (separator == null) {
                break;
            }
            if (separator.getFirst() == null) {
                initialUnclosed = initialTable.addSeparator(separator.getThird(), membershipOracle);
            }
            else {
                callUnclosed.put(
                        separator.getFirst(),
                        callTables.get(separator.getFirst())
                                .addSeparator(Pair.of(separator.getSecond(), separator.getThird()), membershipOracle)
                );
//                System.out.println(callTables.get(separator.getFirst()));
            }
//            System.out.println(ceQuery + " " + separator + " enforcing...");
            assert !initialUnclosed.isEmpty() || !callUnclosed.values().stream().allMatch(List::isEmpty);
            enforceTables();
        }

//        printTables();
        return true;
    }

    @Override
    public SEVPA<?, I> getHypothesisModel() {
        DefaultNSEVPA<I> sevpa = new DefaultNSEVPA<>(alphabet);

        Map<RegularRow<I>, Location> initialLocations = new HashMap<>();
        for (RegularRow<I> q: initialTable.getRepresentativeRows()) {
            if (q.getLabel().equals(Word.epsilon())) {
                initialLocations.put(q, sevpa.addInitialState(initialTable.isFinal(q)));
            } else {
                initialLocations.put(q, sevpa.addState(initialTable.isFinal(q)));
            }
        }

        Map<I, Map<RegularRow<I>, Location>> callLocations = new HashMap<>();
        for (I c: alphabet.getCallAlphabet()) {
            callLocations.put(c, new HashMap<>());
            for (RegularRow<I> q: callTables.get(c).getRepresentativeRows()) {
                if (q.getLabel().equals(Word.epsilon())) {
                    callLocations.get(c).put(q, sevpa.addModuleEntryLocation(c, false));
                } else {
                    callLocations.get(c).put(q, sevpa.addLocation(c, false));
                }
            }
        }

        for (RegularRow<I> q: initialTable.getRepresentativeRows()) {
            for (Word<I> symbol: tableAlphabet) {
                RegularRow<I> successor = initialTable.getSuccessor(q, symbol);
                if (symbol.size() == 1) {
                    sevpa.setInternalSuccessor(
                            initialLocations.get(q),
                            symbol.getSymbol(0),
                            initialLocations.get(successor)
                    );
                } else {
                    I callSymbol = symbol.getSymbol(0);
                    Word<I> locationReturn = symbol.subWord(1, symbol.size()-1);
                    I returnSymbol = symbol.getSymbol(symbol.size()-1);
                    // System.out.println(callSymbol + "-" +  locationReturn + "-" + returnSymbol);
                    // System.out.println(callLocations.get(callSymbol).get(locationReturn));
                    // System.out.println(callLocations.get(callSymbol));
                    // System.out.println(initialLocations.get(successor));

                    sevpa.setReturnSuccessor(
                            callLocations.get(callSymbol).get(callTables.get(callSymbol).getRepresentative(locationReturn)),
                            returnSymbol,
                            sevpa.encodeStackSym(initialLocations.get(q), callSymbol),
                            initialLocations.get(successor)
                    );
                }
            }
        }

        for (I c: alphabet.getCallAlphabet()) {
            for (RegularRow<I> q: callTables.get(c).getRepresentativeRows()) {
                for (Word<I> symbol: tableAlphabet) {
                    RegularRow<I> successor = callTables.get(c).getSuccessor(q, symbol);
                    if (symbol.size() == 1) {
                        sevpa.setInternalSuccessor(
                                callLocations.get(c).get(q),
                                symbol.getSymbol(0),
                                callLocations.get(c).get(successor)
                        );
                    } else {
                        I callSymbol = symbol.getSymbol(0);
                        Word<I> locationReturn = symbol.subWord(1, symbol.size()-1);
                        I returnSymbol = symbol.getSymbol(symbol.size()-1);
                        // System.out.println(callSymbol + " - " +  locationReturn + " - " + returnSymbol);

                        sevpa.setReturnSuccessor(
                                callLocations.get(callSymbol).get(callTables.get(callSymbol).getRepresentative(locationReturn)),
                                returnSymbol,
                                sevpa.encodeStackSym(callLocations.get(c).get(q), callSymbol),
                                callLocations.get(c).get(successor)
                        );
                    }
                }
            }
        }

        return sevpa;
//        DefaultNSEVPA<I> sevpa = new DefaultNSEVPA<>(alphabet);
//
//        Map<Word<I>, Location> initialLocations = new HashMap<>();
//        for (Word<I> q: initialTable.getQ()) {
//            if (q.equals(Word.epsilon())) {
//                initialLocations.put(
//                        q, sevpa.addInitialState(initialTable.isFinal(q)));
//            } else {
//                initialLocations.put(q, sevpa.addState(initialTable.isFinal(q)));
//            }
//        }
//
//        Map<I, Map<Word<I>, Location>> callLocations = new HashMap<>();
//        for (I c: alphabet.getCallAlphabet()) {
//            callLocations.put(c, new HashMap<>());
//            for (Word<I> q: callTables.get(c).getQ()) {
//                if (q.equals(Word.epsilon())) {
//                    callLocations.get(c).put(
//                            q, sevpa.addModuleEntryLocation(c, false));
//                } else {
//                    callLocations.get(c).put(
//                            q, sevpa.addLocation(c, false));
//                }
//            }
//        }
//
//        for (Word<I> q: initialTable.getQ()) {
//            Map<Word<I>,Word<I>> symbToSucc = initialTable.getSuccessorList(q);
//            for (Map.Entry<Word<I>,Word<I>> entry: symbToSucc.entrySet()) {
//                Word<I> symbol = entry.getKey();
//                Word<I> successor = entry.getValue();
//                if (symbol.size() == 1) {
//                    sevpa.setInternalSuccessor(
//                            initialLocations.get(q),
//                            symbol.getSymbol(0),
//                            initialLocations.get(successor)
//                    );
//                } else {
//                    I callSymbol = symbol.getSymbol(0);
//                    Word<I> locationReturn = symbol.subWord(1, symbol.size()-1);
//                    I returnSymbol = symbol.getSymbol(symbol.size()-1);
//                    // System.out.println(callSymbol + "-" +  locationReturn + "-" + returnSymbol);
//                    // System.out.println(callLocations.get(callSymbol).get(locationReturn));
//                    // System.out.println(callLocations.get(callSymbol));
//                    // System.out.println(initialLocations.get(successor));
//
//                    sevpa.setReturnSuccessor(
//                            callLocations.get(callSymbol).get(locationReturn),
//                            returnSymbol,
//                            sevpa.encodeStackSym(initialLocations.get(q), callSymbol),
//                            initialLocations.get(successor)
//                    );
//                }
//            }
//        }
//
//        for (I c: alphabet.getCallAlphabet()) {
//            for (Word<I> q: callTables.get(c).getQ()) {
//                Map<Word<I>,Word<I>> symbToSucc = callTables.get(c).getSuccessorList(q);
//                for (Map.Entry<Word<I>,Word<I>> entry: symbToSucc.entrySet()) {
//                    Word<I> symbol = entry.getKey();
//                    Word<I> successor = entry.getValue();
//                    if (symbol.size() == 1) {
//                        sevpa.setInternalSuccessor(
//                                callLocations.get(c).get(q),
//                                symbol.getSymbol(0),
//                                callLocations.get(c).get(successor)
//                        );
//                    } else {
//                        I callSymbol = symbol.getSymbol(0);
//                        Word<I> locationReturn = symbol.subWord(1, symbol.size()-1);
//                        I returnSymbol = symbol.getSymbol(symbol.size()-1);
//                        // System.out.println(callSymbol + " - " +  locationReturn + " - " + returnSymbol);
//
//                        sevpa.setReturnSuccessor(
//                                callLocations.get(callSymbol).get(locationReturn),
//                                returnSymbol,
//                                sevpa.encodeStackSym(callLocations.get(c).get(q), callSymbol),
//                                callLocations.get(c).get(successor)
//                        );
//                    }
//                }
//            }
//        }
//
//        return sevpa;
    }

    @Override
    public boolean hasLearningProcessStarted() {
        return this.initialTable.isInitialized();
    }

    public void addWordSymbols(List<Word<I>> symbols) {
        tableAlphabet.asGrowingAlphabetOrThrowException().addAll(symbols);

        initialUnclosed.addAll(initialTable.addAlphabetSymbols(symbols, membershipOracle));
        for (I c: alphabet.getCallAlphabet()) {
            callUnclosed.get(c).addAll(
                    callTables.get(c).addAlphabetSymbols(symbols, membershipOracle)
            );
        }
    }

    public void printTables(){
        System.out.println("Initial Table:");
        System.out.println(initialTable);

        System.out.println("Call tables:");
        for (I c: callTables.keySet()) {
            System.out.println("\t Tables linked to " + c);
            System.out.println(callTables.get(c));
        }
    }

    public Triple<I, Word<I>, Word<I>> getSeparator(Word<I> cx, boolean positiveCX) {
        int low = 1;
        int high = cx.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (getSeparatorFromIdx(cx, mid, positiveCX) == null) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return getSeparatorFromIdx(cx, low, positiveCX);
    }

    private Triple<I, Word<I>, Word<I>> getSeparatorFromIdx(Word<I> cx, int maxIdx, boolean positiveCX) {
        Word<I> state = Word.epsilon();
        List<Pair<I, Word<I>>> stack = new ArrayList<>();
        I callSymbol = null;
        for (int i = 0; i < maxIdx; i++) {
            if (alphabet.isCallSymbol(
                    cx.getSymbol(i)
            )) {
                stack.add(Pair.of(callSymbol, state));
                state = Word.epsilon();
                callSymbol = cx.getSymbol(i);
            } else if (alphabet.isReturnSymbol(
                    cx.getSymbol(i)
            )) {
                Pair<I, Word<I>> stackSymbol = stack.removeLast();
                AbstractRegularObservationTable<I, Word<I>, I, ?> table =
                        stackSymbol.getFirst() == null ? initialTable : callTables.get(stackSymbol.getFirst());
                state = table.getSuccessor(
                        stackSymbol.getSecond(),
                        Word.fromWords(
                                Word.fromLetter(callSymbol),
                                state,
                                Word.fromLetter(cx.getSymbol(i))
                        )
                );
                callSymbol = stackSymbol.getFirst();
            } else {
                AbstractRegularObservationTable<I, Word<I>, I, ?> table =
                        callSymbol == null ? initialTable : callTables.get(callSymbol);
                state = table.getSuccessor(
                        state,
                        Word.fromLetter(cx.getSymbol(i))
                );
            }
        }
        Word<I> stackWord = Word.epsilon();
        for (Pair<I, Word<I>> stackElement: stack) {
            if (stackElement.getFirst() != null) {
                stackWord = stackWord.append(stackElement.getFirst());
            }
            stackWord = Word.fromWords(stackWord, stackElement.getSecond());
        }
        if (callSymbol != null) {
            stackWord = stackWord.append(callSymbol);
        }
        Word<I> wordToTest = Word.fromWords(
                stackWord,
                state,
                cx.suffix(cx.size()-maxIdx));
        if (positiveCX != membershipOracle.answerQuery(wordToTest)) {
            return Triple.of(callSymbol, stackWord, cx.suffix(cx.size()-maxIdx));
        }
        return null;
    }
}
