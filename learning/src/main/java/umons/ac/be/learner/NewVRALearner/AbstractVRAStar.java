package umons.ac.be.learner.NewVRALearner;

import de.learnlib.LearnerStateTracker;
import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;
import umons.ac.be.learnlibObservationTable.VRA.RecursiveObservationTable;
import umons.ac.be.learnlibObservationTable.VRA.RecursiveObservationTableIsomorphic;
import umons.ac.be.learnlibObservationTable.VRA.VRAInitialObservationTable;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.DefaultVRAlphabet;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public abstract class AbstractVRAStar<I>  implements LearningAlgorithm<VRA<?, I, ?>, I, Boolean>,
                                    LearnerStateTracker {
    protected VPAlphabet<I> alphabet;
    protected Alphabet<LearningSymbol<I>> tableAlphabet = new GrowingMapAlphabet<>();

    protected MembershipOracle<I, Boolean> membershipOracle;

    protected VRAInitialObservationTable<I> initialTable;
    protected Map<Pair<I, I>, RecursiveObservationTable<I>> recursiveTables = new HashMap<>();

    protected List<RegularRow<LearningSymbol<I>>> initialUnclosed;
    protected Map<Pair<I, I>, List<RegularRow<LearningSymbol<I>>>> recursiveUnclosed = new HashMap<>();
    protected List<LearningSymbol<I>> newSymbols = new ArrayList<>();

    protected Map<LearningSymbol<I>, BitSet> proceduralSymbolToContext = new HashMap<>();
    protected Map<LearningSymbol<I>, I> learningSymbolToProceduralSymbol = new HashMap<>();

    boolean wasRefined;

    protected AbstractVRAStar(VPAlphabet<I> alphabet,
                              MembershipOracle<I, Boolean> membershipOracle,
                              int query_batch_size) {
        this.alphabet = alphabet;
        this.membershipOracle = membershipOracle;

        for (I i : alphabet.getInternalAlphabet()) {
            tableAlphabet.add(LearningSymbol.getLearningSymbol(i));
        }

        this.initialTable =  new VRAInitialObservationTable<>(tableAlphabet, query_batch_size);
    }

    @Override
    public boolean hasLearningProcessStarted() {
        return initialTable.isInitialized();
    }

    @Override
    public void startLearning() {
        requireLearningProcessNotStarted();
        initialUnclosed = initialTable.initialize(membershipOracle);
        for (I c: alphabet.getCallAlphabet()) {
            for (I r : alphabet.getReturnAlphabet()) {
                recursiveUnclosed.put(
                        Pair.of(c, r),
                        new ArrayList<>(recursiveTables.get(Pair.of(c, r)).initialize(membershipOracle))
                );
            }
        }

//        printTables();
        enforceTables();
    }

    protected void enforceTables() {
        if (!initialUnclosed.isEmpty() ||
                !recursiveUnclosed.values().stream().allMatch(List::isEmpty)) {
            wasRefined = true;
            closeTables();
            enforceTables();
        }
        getNewSymbols();
        if (!newSymbols.isEmpty()) {
            wasRefined = true;
            addSymbolsToTable();
            enforceTables();
        }
    }

    protected void closeTables() {
        initialUnclosed = initialTable.toRepresentative(initialUnclosed, membershipOracle);
        for (I c: alphabet.getCallAlphabet()) {
            for (I r : alphabet.getReturnAlphabet()) {
                List<RegularRow<LearningSymbol<I>>> unclosed = recursiveUnclosed.get(Pair.of(c,r));
                recursiveUnclosed.put(Pair.of(c, r), new ArrayList<>());
                recursiveUnclosed.get(Pair.of(c, r)).addAll(
                        recursiveTables.get(Pair.of(c, r)).toRepresentative(unclosed, membershipOracle));
            }
        }
    }

    protected void addSymbolsToTable() {
        List<LearningSymbol<I>> symbolsToAdd = newSymbols.stream().toList();
        newSymbols = new ArrayList<>();
        initialUnclosed.addAll(initialTable.addAlphabetSymbols(symbolsToAdd, membershipOracle));
        for (I c: alphabet.getCallAlphabet()) {
            for (I r : alphabet.getReturnAlphabet()) {
                recursiveUnclosed.get(Pair.of(c, r)).addAll(
                        recursiveTables.get(Pair.of(c, r)).addAlphabetSymbols(symbolsToAdd, membershipOracle)
                );
            }
        }
    }

    private void getNewSymbols() {
        for (I c: alphabet.getCallAlphabet()) {
            for (I r : alphabet.getReturnAlphabet()) {
                Map<Word<LearningSymbol<I>>, BitSet> newProcSymbols = recursiveTables.get(Pair.of(c, r)).getNewProceduralSymbols();
                for (Map.Entry<Word<LearningSymbol<I>>, BitSet> e: newProcSymbols.entrySet()) {
                    LearningSymbol<I> newProcSymbol = LearningSymbol.getLearningSymbol(c, e.getKey(), r);
                    newSymbols.add(newProcSymbol);
                    proceduralSymbolToContext.put(newProcSymbol, e.getValue());
                    learningSymbolToProceduralSymbol.put(newProcSymbol, (I) newProcSymbol.toString());
                    tableAlphabet.add(newProcSymbol);
                }
            }
        }
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<I, Boolean> defaultQuery) {
        wasRefined = false;
//        printTables();
        Word<LearningSymbol<I>> regularCX = getRegularWord(
                defaultQuery.getInput(),
                Word.epsilon(),
                Word.epsilon(),
                defaultQuery.getOutput());

//        System.out.println("Processing " + (defaultQuery.getOutput() ? "positive" : "negative") +
//                " counterexample: " + defaultQuery.getInput() + " with regular proj " + regularCX);
//        System.out.println(initialTable.getEquivalentRep(regularCX));
//        System.out.println(recursiveTables);
        assert defaultQuery.getOutput() == membershipOracle.answerQuery(LearningSymbol.expand(regularCX));

        while(true) {
            Word<LearningSymbol<I>> separator = getInitialSeparator(regularCX, defaultQuery.getOutput());
            if (separator == null) {
                break;
            }
            initialUnclosed = initialTable.addSeparator(separator, membershipOracle);
//            System.out.println(initialTable);
//            System.out.println("Added separators " + separator + " when processing CX " + regularCX);
            assert !initialUnclosed.isEmpty();
            enforceTables();
        }
        return wasRefined;
    }

    Word<LearningSymbol<I>> getInitialSeparator(Word<LearningSymbol<I>> z, boolean positiveCX) {
        int low = 1;
        int high = z.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (getInitialSeparatorFromIdx(z, mid, positiveCX) == null) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return getInitialSeparatorFromIdx(z, low, positiveCX);
    }

    Word<LearningSymbol<I>> getInitialSeparatorFromIdx(Word<LearningSymbol<I>> z,  int idx, boolean positiveCX) {
        Word<LearningSymbol<I>> prefix = z.prefix(idx);
        Word<LearningSymbol<I>> suffix = z.suffix(z.length() - idx);
        Word<LearningSymbol<I>> prefEquiv = initialTable.getEquivalentRep(prefix);
        if(membershipOracle.answerQuery(LearningSymbol.expand(Word.fromWords(prefEquiv, suffix))) != positiveCX) {
            return suffix;
        }
        return null;
    }

    abstract LearningSymbol<I> processRecursiveCounterExample(Word<I> prefix, I callSymbol, Word<I> cx, I returnSymbol, Word<I> suffix, boolean isPositiveCX);


    protected Word<LearningSymbol<I>> getRegularWord(Word<I> cx, Word<I> prefix, Word<I> suffix, boolean isPositiveCX) {
        Word<LearningSymbol<I>> regularWord = Word.epsilon();
        for (int i = 0; i < cx.size(); i++) {
            if (alphabet.isCallSymbol(cx.getSymbol(i))) {
                int indexReturn = getMatchingReturnIndex(cx, i);
                regularWord = regularWord.append(processRecursiveCounterExample(
                        Word.fromWords(prefix, LearningSymbol.expand(regularWord)),
                        cx.getSymbol(i),
                        cx.subWord(i+1, indexReturn),
                        cx.getSymbol(indexReturn),
                        Word.fromWords(cx.suffix(cx.size()-indexReturn-1), suffix),
                        isPositiveCX
                ));
                i = indexReturn;
            }
            else {
                regularWord = regularWord.append(LearningSymbol.getLearningSymbol(cx.getSymbol(i)));
            }
        }
        return regularWord;
    }


    protected int getMatchingReturnIndex(Word<I> word, int callIndex) {
        int unmatchedCall = 1;
        for (int i = callIndex+1; i < word.size(); i++) {
            if (alphabet.isCallSymbol(word.getSymbol(i))) {
                unmatchedCall++;
            }
            if (alphabet.isReturnSymbol(word.getSymbol(i))) {
                unmatchedCall--;
                if (unmatchedCall == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected FastDFA<I> constructHypothesisInitialDFA(Alphabet<I> automatonAlphabet) {
        FastDFA<I> initialAutomaton = new FastDFA<>(automatonAlphabet);
        Map<RegularRow<LearningSymbol<I>>, FastDFAState> rowToState = new HashMap<>();

        for (RegularRow<LearningSymbol<I>> q: initialTable.getRepresentativeRows()) {
            if (q.getLabel().equals(Word.epsilon())) {
                rowToState.put(q, initialAutomaton.addInitialState(initialTable.isFinal(q)));
            } else {
                rowToState.put(q, initialAutomaton.addState(initialTable.isFinal(q)));
            }
        }

        for (RegularRow<LearningSymbol<I>> q: initialTable.getRepresentativeRows()) {
            for (LearningSymbol<I> s: tableAlphabet) {
                RegularRow<LearningSymbol<I>> succ = initialTable.getSuccessor(q, s);
                I symbolTrans;
                if (learningSymbolToProceduralSymbol.containsKey(s)) {
                    symbolTrans = learningSymbolToProceduralSymbol.get(s);
                } else {
                    symbolTrans = s.expand().getSymbol(0);
                }
//                System.out.println("Adding transition from " + q + " to " + succ + " reading " + symbolTrans);
                initialAutomaton.addTransition(
                        rowToState.get(q),
                        symbolTrans,
                        rowToState.get(succ)
                );
            }
        }

        return initialAutomaton;
    }

    @Override
    public VRA<?, I, ?> getHypothesisModel() {
        Alphabet<I> automatonAlphabet = new GrowingMapAlphabet<>(alphabet.getInternalAlphabet());
        automatonAlphabet.addAll(learningSymbolToProceduralSymbol.values());
//        System.out.println(automatonAlphabet);
//        System.out.println(learningSymbolToProceduralSymbol.values());

        VRAlphabet<I> vrAlphabet = new DefaultVRAlphabet<>(alphabet);

        HashMap<I, FastDFA<I>> procedures = new HashMap<>();
        for (LearningSymbol<I> procSymbol : proceduralSymbolToContext.keySet()) {
            Word<I> exp = procSymbol.expand();
            I c = exp.getSymbol(0);
            I r = exp.getSymbol(exp.size()-1);
            procedures.put(
                    learningSymbolToProceduralSymbol.get(procSymbol),
                    constructHypothesisDFA(automatonAlphabet, c, r, procSymbol)
            );
            vrAlphabet.addProceduralSymbol(learningSymbolToProceduralSymbol.get(procSymbol), c, r);
        }
        return new DefaultVRAwithDFA<>(vrAlphabet, procedures, constructHypothesisInitialDFA(automatonAlphabet));
    }

    abstract FastDFA<I> constructHypothesisDFA(Alphabet<I> alphabet, I c, I r, LearningSymbol<I> procSymbol);


    public void printTables() {
        System.out.println("Starting Table:");
        System.out.println(initialTable);
        System.out.println("\nRecursive Tables:");
        for (RecursiveObservationTable<I> table: recursiveTables.values()) {
            System.out.println(table + "\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
        }
    }
}
