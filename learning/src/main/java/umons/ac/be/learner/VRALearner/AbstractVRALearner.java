package umons.ac.be.learner.VRALearner;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import net.automatalib.visualization.Visualization;
import umons.ac.be.learner.Learner;
import umons.ac.be.ObservationTable.RecursiveObservationTable.RecursiveObservationTable;
import umons.ac.be.ObservationTable.RegularObservationTable;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractVRALearner<I> implements Learner<I, VRA<FastDFAState, I, DFA<FastDFAState, I>>> {
    protected VRAlphabet<I> alphabet;
    protected MembershipOracle<I, Boolean> membershipOracle;
    protected EquivalenceOracle<VRA<FastDFAState, I, DFA<FastDFAState, I>>, I, Boolean> equivalenceOracle;
//    protected Oracle<I, AbstractVRAwithDFA<?, I>> oracle;

    protected HashMap<I, Word<I>> proceduralSymbolToWord = new HashMap<>();
    protected HashMap<Word<I>, I> wordToProceduralSymbol= new HashMap<>();
    protected RegularObservationTable<I, AbstractVRALearner<I>> startingObservationTable;
    protected HashMap<Pair<I, I>, RecursiveObservationTable<I>> recursiveObservationTables = new HashMap<>();

    int ProceduralSymbolCounter = 0;
    int numberOfMQ = 0;
    int numberOfEQ = 0;


//    public VRALearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
//        this.alphabet = alphabet;
//        this.oracle = oracle;
//
//        startingObservationTable = new RegularObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this);
//    }

    public AbstractVRALearner(VRAlphabet<I> alphabet,
                              MembershipOracle<I, Boolean> membershipOracle,
                              EquivalenceOracle<VRA<FastDFAState, I, DFA<FastDFAState, I>>, I, Boolean> equivalenceOracle) {
        this.alphabet = alphabet;
        this.membershipOracle = membershipOracle;
        this.equivalenceOracle = equivalenceOracle;

        startingObservationTable = new RegularObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this);
    }

    private boolean askMembership(Word<I> input) {
        numberOfMQ++;
        boolean answer =  membershipOracle.answerQuery(input);
//        System.out.println("MQ of : " + input + " = " + answer);
        return answer;
    }

    public boolean recursiveMembershipQuery(Word<I> prefix, I callSymbol, Word<I> regularWord, I returnSymbol, Word<I> suffix) {
        return askMembership(Word.fromWords(
                prefix, Word.fromLetter(callSymbol), expand(regularWord), Word.fromLetter(returnSymbol), suffix
        ));
    }

    @Override
    public boolean askMembershipQuery(Word<I> regularWord) {
        return askMembership(expand(regularWord));
    }

    public void addProceduralSymbol(Word<I> regularWord, I callSymbol, I returnSymbol) {
        I newSymbol = generateProceduralSymbol(expand(regularWord), callSymbol, returnSymbol);
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
//        return (I) ("J^(" + callSymbol + "," + returnSymbol + ")_[" + regularWord + "]");
        return (I) ("J" + ProceduralSymbolCounter);
    }

    public Word<I> expand(Word<I> regularWord) {
        Word<I> extendedWord = Word.epsilon();
        for (I symbol : regularWord) {
            if (alphabet.isProceduralSymbol(symbol)) {
                extendedWord = Word.fromWords(extendedWord, expand(proceduralSymbolToWord.get(symbol)));
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
    public VRA<FastDFAState, I, DFA<FastDFAState, I>> constructHypothesis() {
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

    @Override
    public VRA<FastDFAState, I, DFA<FastDFAState, I>> learn() {
        startingObservationTable.initialize();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            table.initialize();
        }
        enforce();
        for (int i = 0; i < 1000; i++) {
//            printTables();
//            displayStats();
            System.out.println("Searching a counterexample... " + numberOfEQ);
            VRA<FastDFAState, I, DFA<FastDFAState, I>> hypothesis = constructHypothesis();
//            System.out.println(hypothesis);
//            Visualization.visualize(hypothesis.removeBinStatesAndAutomata());
//            hypothesis.visualizeIndividually();
            numberOfEQ++;
            Query<I, Boolean> cx = equivalenceOracle.findCounterExample(hypothesis, null);
//            System.out.println("Processing counterexample: " + cx);
            if (cx == null) {
                break;
            } else {
                System.out.println("Processing " + cx);
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
//        printTables();
        Word<I> regularCX = getRegularWord(cx, Word.epsilon(), Word.epsilon());
//        System.out.println("Processing counterexample: " + cx + " with regular proj " + regularCX);
        startingObservationTable.addRepresentative(regularCX);
        startingObservationTable.enforce();
    }


    protected I processRecursiveCounterExample(Word<I> prefix, I callSymbol, Word<I> cx, I returnSymbol, Word<I> suffix) {
        RecursiveObservationTable<I> recObsTab = recursiveObservationTables.get(Pair.of(callSymbol, returnSymbol));
        Word<I> regularWord = getRegularWord(cx,
                Word.fromWords(prefix, Word.fromLetter(callSymbol)),
                Word.fromWords(Word.fromLetter(returnSymbol), suffix)
        );
        recObsTab.addRepresentative(regularWord);
        recObsTab.addContext(prefix, suffix);
        enforce();

//        System.out.println(recObsTab.getRecursiveEquivalentClass());
//        System.out.println(recObsTab);
//        System.out.println("Processed counterexample: " + cx);
//        System.out.println("\twith recursive equivalent " + wordToProceduralSymbol.get(
//                Word.fromWords(Word.fromLetter(callSymbol),
//                        recObsTab.getRecursiveEquivalent(regularWord),
//                        Word.fromLetter(returnSymbol)
//                )));
//        System.out.println("\twith regular proj " + regularWord);

        return wordToProceduralSymbol.get(
                Word.fromWords(Word.fromLetter(callSymbol),
                        recObsTab.getRecursiveEquivalent(regularWord),
                        Word.fromLetter(returnSymbol)
                ));
    }


    protected Word<I> getRegularWord(Word<I> cx, Word<I> prefix, Word<I> suffix) {
        Word<I> regularWord = Word.epsilon();
        for (int i = 0; i < cx.size(); i++) {
            if (alphabet.isCallSymbol(cx.getSymbol(i))) {
                int indexReturn = getMatchingReturnIndex(cx, i);
//                System.out.println("Processing "+cx+ " on context ( " + prefix + " , " + suffix + " )\n\t\t" +
//                        Word.fromWords(prefix, cx.prefix(i)) + "\t\t" + cx.getSymbol(i) + "\t\t" +
//                        cx.subWord(i+1, indexReturn) + "\t\t" + cx.getSymbol(indexReturn) + "\t\t" +
//                        Word.fromWords(cx.suffix(cx.size()-indexReturn-1), suffix)
//                );
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

    public void printTables() {
        System.out.println("Starting Table:");
        System.out.println(startingObservationTable);
        System.out.println("\nRecursive Tables:");
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            System.out.println(table + "\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
        }
    }

    @Override
    public void displayStats(){
        System.out.println("Number of membership queries: " + numberOfMQ);
        System.out.println("Number of equivalence queries: " + numberOfEQ);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Number of membership queries: ").append(numberOfMQ).append("\n")
                .append("Number of equivalence queries: ").append(numberOfEQ).append("\n")
                .append("Procedural alphabet:").append("\n");
        for (Map.Entry<I, Word<I>> entry: proceduralSymbolToWord.entrySet()) {
            sb.append("\t").append(entry.getKey()).append(" linked to word: ").append(entry.getValue()).append("\n");
        }
        sb.append("\n").append("Starting observation table:").append("\n")
                .append(startingObservationTable).append("\n\n")
                .append("Recursive observation tables:").append("\n");
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            sb.append(table).append("\n\n");
        }

        return sb.toString();
    }

    public int getNumberMQ() {
        return  numberOfMQ;
    }

    public int getNumberEQ() {
        return  numberOfEQ;
    }
}
