package learner.VRA;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import learner.Learner;
import learner.ObservationTable.RecursiveObservationTable.RecursiveObservationTable;
import learner.ObservationTable.RegularObservationTable;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.common.util.Pair;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.HashMap;

public abstract class VRALearner<I> implements Learner<I, AbstractVRAwithDFA<?, I>> {
    protected VRAlphabet<I> alphabet;
    protected MembershipOracle<I, Boolean> membershipOracle;
    protected EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle;
//    protected Oracle<I, AbstractVRAwithDFA<?, I>> oracle;

    protected HashMap<I, Word<I>> proceduralSymbolToWord = new HashMap<>();
    protected HashMap<Word<I>, I> wordToProceduralSymbol= new HashMap<>();
    protected RegularObservationTable<I> startingObservationTable;
    protected HashMap<Pair<I, I>, RecursiveObservationTable<I>> recursiveObservationTables = new HashMap<>();

    int ProceduralSymbolCounter = 0;

//    public VRALearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
//        this.alphabet = alphabet;
//        this.oracle = oracle;
//
//        startingObservationTable = new RegularObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this);
//    }

    public VRALearner(VRAlphabet<I> alphabet,
                      MembershipOracle<I, Boolean> membershipOracle,
                      EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle) {
        this.alphabet = alphabet;
        this.membershipOracle = membershipOracle;
        this.equivalenceOracle = equivalenceOracle;

        startingObservationTable = new RegularObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this);
    }

    private boolean askMembership(Word<I> input) {
        boolean answer =  membershipOracle.answerQuery(input);
        System.out.println("MQ of : " + input + " = " + answer);
        return answer;
    }

    public boolean recursiveMembershipQuery(Word<I> prefix, I callSymbol, Word<I> regularWord, I returnSymbol, Word<I> suffix) {
//        boolean answer = oracle.MembershipQuery(Word.fromWords(
//                prefix, Word.fromLetter(callSymbol), extend(regularWord), Word.fromLetter(returnSymbol), suffix
//        ));
        return askMembership(Word.fromWords(
                prefix, Word.fromLetter(callSymbol), extend(regularWord), Word.fromLetter(returnSymbol), suffix
        ));
    }

    @Override
    public boolean askMembershipQuery(Word<I> regularWord) {
        return askMembership(extend(regularWord));
    }

    public void addProceduralSymbol(Word<I> regularWord, I callSymbol, I returnSymbol) {
        I newSymbol = generateProceduralSymbol(extend(regularWord), callSymbol, returnSymbol);
        Word<I> recEquivClass = Word.fromWords(Word.fromLetter(callSymbol), regularWord, Word.fromLetter(returnSymbol));

        System.out.println("Adding procedural symbol " + newSymbol + " linked to " + recEquivClass);

        alphabet.addProceduralSymbol(newSymbol, callSymbol, returnSymbol);
        proceduralSymbolToWord.put(newSymbol, recEquivClass);
        wordToProceduralSymbol.put(recEquivClass, newSymbol);

        startingObservationTable.addSymbol(newSymbol);
        for(RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            table.addSymbol(newSymbol);
        }
        enforce();
    }

    public I generateProceduralSymbol(Word<I> regularWord, I callSymbol, I returnSymbol) {
        ProceduralSymbolCounter += 1;
        return (I) ("J^(" + callSymbol + "," + returnSymbol + ")_[" + regularWord + "]");
    }

//    public List<Word<I>> getRecursiveEquivalenceClasses(I callSymbol, I returnSymbol) {
//        Alphabet<I> proceduralSymbol = alphabet.getProceduralAlphabetFromCallAndReturn(callSymbol, returnSymbol);
//        List<Word<I>> result = new ArrayList<>();
//        for (I symbol : proceduralSymbol) {
//            result.add(proceduralSymbolToWord.get(symbol));
//        }
//        return result;
//    }

    public Word<I> extend(Word<I> regularWord) {
        Word<I> extendedWord = Word.epsilon();
        for (I symbol : regularWord) {
            if (alphabet.isProceduralSymbol(symbol)) {
                extendedWord = Word.fromWords(extendedWord, extend(proceduralSymbolToWord.get(symbol)));
            }
            else {
                extendedWord = extendedWord.append(symbol);
            }
        }
        // System.out.println(regularWord + " extended to " + extendedWord);
        return extendedWord;
    }

    public int getIndexOfRecursiveEquivalenceClass(Word<I> recEquivClass) {
        return alphabet.getSymbolIndex(wordToProceduralSymbol.get(recEquivClass));
    }

    @Override
    public AbstractVRAwithDFA<?,I> constructHypothesis() {
        FastDFA<I> startingAutomaton = startingObservationTable.constructHypothesis();
        HashMap<Word<I>, FastDFA<I>> hypotheses = new HashMap<>();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            hypotheses.putAll(table.constructHypothesis());
        }
        HashMap<I, FastDFA<I>> procedures = new HashMap<>();
        procedures.put((I) "S", startingAutomaton);
        for (Word<I> recEquivClass : hypotheses.keySet()) {
            procedures.put(wordToProceduralSymbol.get(recEquivClass), hypotheses.get(recEquivClass));
        }
        return new DefaultVRAwithDFA<>(alphabet, procedures, startingAutomaton);
    }

//    public AbstractVRAwithDFA<?, I> constructReducedHypothesis() {
//        FastDFA<I> startingAutomaton = startingObservationTable.constructHypothesis();
//        HashMap<Word<I>, FastDFA<I>> hypotheses = new HashMap<>();
//        for (IsomorphicRecursiveObservationTable<I> table: recursiveObservationTables.values()) {
//            hypotheses.putAll(table.constructReducedHypotheses());
//        }
//        HashMap<I, FastDFA<I>> procedures = new HashMap<>();
//        procedures.put((I) "S", startingAutomaton);
//        for (Word<I> recEquivClass : hypotheses.keySet()) {
//            procedures.put(wordToProceduralSymbol.get(recEquivClass), hypotheses.get(recEquivClass));
//        }
//        return new DefaultVRAwithDFA<>(alphabet, procedures, startingAutomaton);
//    }

    @Override
    public AbstractVRAwithDFA<?, I> learn() {
        startingObservationTable.initialize();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            table.initialize();
        }
        enforce();
        for (int i = 0; i < 1000; i++) {
            printTables();
            AbstractVRAwithDFA<?, I> hypothesis = constructHypothesis();
//            Visualization.visualize(hypothesis);
//            Word<I> cx = oracle.EquivalenceQuery(hypothesis);
            Query<I, Boolean> cx = equivalenceOracle.findCounterExample(hypothesis, null);
            System.out.println("Processing counterexample: " + cx);
            if (cx == null) {
                break;
            } else {
                processCounterExample(cx.getInput());
            }
        }
//        oracle.displayStats();
        return constructHypothesis();
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

    public void enforce() {
        startingObservationTable.enforce();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            table.enforce();
        }
    }

    @Override
    public void processCounterExample(Word<I> cx) {
        Word<I> regularCX = getRegularWord(cx, Word.epsilon(), Word.epsilon());
        System.out.println("Processing counterexample: " + cx + " with regular proj " + regularCX);
        startingObservationTable.addRepresentative(regularCX);
        startingObservationTable.enforce();
    }


    private I processRecursiveCounterExample(Word<I> prefix, I callSymbol, Word<I> cx, I returnSymbol, Word<I> suffix) {
        RecursiveObservationTable<I> recObsTab = recursiveObservationTables.get(Pair.of(callSymbol, returnSymbol));
        Word<I> regularWord = getRegularWord(cx,
                Word.fromWords(prefix, Word.fromLetter(callSymbol)),
                Word.fromWords(suffix, Word.fromLetter(returnSymbol)));
        recObsTab.addRepresentative(regularWord);
        recObsTab.addContext(prefix, suffix);
        enforce();

//        System.out.println(recObsTab);
        System.out.println("Processed counterexample: " + regularWord +
                " with recursive equivalent " + recObsTab.getRecursiveEquivalent(regularWord) +
                " with regular proj " + wordToProceduralSymbol.get(
                Word.fromWords(Word.fromLetter(callSymbol),
                        recObsTab.getRecursiveEquivalent(regularWord),
                        Word.fromLetter(returnSymbol)
                )) +
                "   " + wordToProceduralSymbol);

        return wordToProceduralSymbol.get(
                Word.fromWords(Word.fromLetter(callSymbol),
                        recObsTab.getRecursiveEquivalent(regularWord),
                        Word.fromLetter(returnSymbol)
                ));
    }


    private Word<I> getRegularWord(Word<I> cx, Word<I> prefix, Word<I> suffix) {
        Word<I> regularWord = Word.epsilon();
        for (int i = 0; i < cx.size(); i++) {
            if (alphabet.isCallSymbol(cx.getSymbol(i))) {
                int indexReturn = getMatchingReturnIndex(cx, i);
                System.out.println(i + "\t\t" +
                        Word.fromWords(prefix, cx.prefix(i)) + "\t\t" + cx.getSymbol(i) + "\t\t" +
                        cx.subWord(i+1, indexReturn) + "\t\t" + cx.getSymbol(indexReturn) + "\t\t" +
                        Word.fromWords(cx.suffix(cx.size()-indexReturn-1), suffix)
                );
                regularWord = regularWord.append(processRecursiveCounterExample(
                        Word.fromWords(prefix, cx.prefix(i)),
                        cx.getSymbol(i),
                        cx.subWord(i+1, indexReturn),
                        cx.getSymbol(indexReturn),
                        Word.fromWords(cx.suffix(cx.size()-indexReturn-1), suffix)
                ));
                i = indexReturn;
            }
            else {
                regularWord = regularWord.append(cx.getSymbol(i));
            }
        }
        return regularWord;
    }

    void printTables() {
        System.out.println("Starting Table:");
        System.out.println(startingObservationTable);
        System.out.println("\nRecursive Tables:");
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            System.out.println(table + "\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
        }
    }
}
