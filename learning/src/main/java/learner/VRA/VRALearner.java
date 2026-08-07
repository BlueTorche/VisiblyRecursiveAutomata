package learner.VRA;

import learner.Learner;
import learner.VRA.isomophicLearning.RecursiveObservationTable;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.common.util.Pair;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import oracle.Oracle;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class VRALearner<I> implements Learner<I, AbstractVRAwithDFA<?, I>> {
    protected VRAlphabet<I> alphabet;
    protected Oracle<I, AbstractVRAwithDFA<?, I>> oracle;

    protected HashMap<I, Word<I>> proceduralSymbolToWord = new HashMap<>();
    protected HashMap<Word<I>, I> wordToProceduralSymbol= new HashMap<>();
    protected RegularObservationTable<I> startingObservationTable;
    protected HashMap<Pair<I, I>, RecursiveObservationTable<I>> recursiveObservationTables = new HashMap<>();

    int ProceduralSymbolCounter = 0;

    public VRALearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
        this.alphabet = alphabet;
        this.oracle = oracle;

        GrowingAlphabet<I> automatonAlphabet = new GrowingMapAlphabet<>(alphabet.getInternalAlphabet());
        startingObservationTable = new RegularObservationTable<>(automatonAlphabet, this);
        for (I call: alphabet.getCallAlphabet()) {
            for (I ret: alphabet.getReturnAlphabet()) {
                recursiveObservationTables.put(
                        Pair.of(call, ret), new RecursiveObservationTable<>(automatonAlphabet, call, ret, this)
                );
                addProceduralSymbol(Word.epsilon(), call, ret);
            }
        }
    }


    public boolean recursiveMembershipQuery(Word<I> prefix, I callSymbol, Word<I> regularWord, I returnSymbol, Word<I> suffix) {
        System.out.println("MQ of : " + prefix + " " + callSymbol + " " + regularWord + " " + returnSymbol + " "+  suffix);
        return oracle.MembershipQuery(Word.fromWords(
                prefix, Word.fromLetter(callSymbol), extend(regularWord), Word.fromLetter(returnSymbol), suffix
        ));
    }

    @Override
    public boolean askMembershipQuery(Word<I> regularWord) {
        System.out.println("MQ of : " + regularWord);
        return oracle.MembershipQuery(extend(regularWord));
    }

    public I addProceduralSymbol(Word<I> regularWord, I callSymbol, I returnSymbol) {
        I newSymbol = generateProceduralSymbol();
        alphabet.addProceduralSymbol(newSymbol, callSymbol, returnSymbol);
        Word<I> recEquivClass = Word.fromWords(Word.fromLetter(callSymbol), regularWord, Word.fromLetter(returnSymbol));
        proceduralSymbolToWord.put(newSymbol, recEquivClass);
        wordToProceduralSymbol.put(recEquivClass, newSymbol);

        startingObservationTable.addSymbol(newSymbol);
        for(RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            table.addSymbol(newSymbol);
        }

        System.out.println("Added procedural symbol " + newSymbol + " linked to " + recEquivClass);
        return newSymbol;
    }

    public I generateProceduralSymbol() {
        ProceduralSymbolCounter += 1;
        return (I) ("J" + ProceduralSymbolCounter);
    }

    public List<Word<I>> getRecursiveEquivalenceClasses(I callSymbol, I returnSymbol) {
        Alphabet<I> proceduralSymbol = alphabet.getProceduralAlphabetFromCallAndReturn(callSymbol, returnSymbol);
        List<Word<I>> result = new ArrayList<>();
        for (I symbol : proceduralSymbol) {
            result.add(proceduralSymbolToWord.get(symbol));
        }
        return result;
    }

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
    public AbstractVRAwithDFA<?, I> constructHypothesis() {
        FastDFA<I> startingAutomaton = startingObservationTable.constructHypothesis();
        HashMap<Word<I>, FastDFA<I>> hypotheses = new HashMap<>();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            hypotheses.putAll(table.constructAllHypotheses());
        }
        HashMap<I, FastDFA<I>> procedures = new HashMap<>();
        procedures.put((I) "S", startingAutomaton);
        for (Word<I> recEquivClass : hypotheses.keySet()) {
            procedures.put(wordToProceduralSymbol.get(recEquivClass), hypotheses.get(recEquivClass));
        }
        return new DefaultVRAwithDFA<>(alphabet, procedures, startingAutomaton);
    }

    public AbstractVRAwithDFA<?, I> constructReducedHypothesis() {
        FastDFA<I> startingAutomaton = startingObservationTable.constructHypothesis();
        HashMap<Word<I>, FastDFA<I>> hypotheses = new HashMap<>();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            hypotheses.putAll(table.constructReducedHypotheses());
        }
        HashMap<I, FastDFA<I>> procedures = new HashMap<>();
        procedures.put((I) "S", startingAutomaton);
        for (Word<I> recEquivClass : hypotheses.keySet()) {
            procedures.put(wordToProceduralSymbol.get(recEquivClass), hypotheses.get(recEquivClass));
        }
        return new DefaultVRAwithDFA<>(alphabet, procedures, startingAutomaton);
    }

    @Override
    public AbstractVRAwithDFA<?, I> learn() {
        startingObservationTable.initialize();
        for (RecursiveObservationTable<I> table: recursiveObservationTables.values()) {
            table.initialize();
        }
        for (int i = 0; i < 1000; i++) {
            AbstractVRAwithDFA<?, I> hypothesis = constructHypothesis();
            Visualization.visualize(hypothesis);
            Word<I> cx = oracle.EquivalenceQuery(hypothesis);
            System.out.println("Processing counterexample: " + cx);
            if (cx == null) {
                break;
            } else {
                processCounterExample(cx);
            }
        }
        oracle.displayStats();
        return constructReducedHypothesis();
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
}
