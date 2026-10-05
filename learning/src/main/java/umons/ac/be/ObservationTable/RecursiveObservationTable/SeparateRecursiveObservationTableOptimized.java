package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.Row.Content.RecursiveContent;
import umons.ac.be.ObservationTable.Row.SeparateRecursiveRow;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.learner.VRALearner.VRASeparateLearnerOptimized;

import java.util.*;

public class SeparateRecursiveObservationTableOptimized<I, L extends VRASeparateLearnerOptimized<I>>
        extends SeparateRecursiveObservationTable<I, L> {
    public SeparateRecursiveObservationTableOptimized(
            GrowingAlphabet<I> inputAlphabet,
            L learner,
            I callSymbol,
            I returnSymbol) {
        super(inputAlphabet, learner, callSymbol, returnSymbol);
    }

//    public Set<Word<I>> getAllEquivalent(Word<I> input) {
//        if (allRows.containsKey(input)) {
//            return Collections.singleton(getRecursiveEquivalent(input));
//        }
//        Set<Word<I>> result = new HashSet<>();
//        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
//            Word<I> equivalent = getTransitions(
//                    (SeparateRecursiveRow<I>) allRows.get(Word.epsilon()),
//                    input,
//                    recEquivClass);
//            if (areRecursiveEquivalent(recEquivClass, equivalent, 0)) {
//                result.add(recEquivClass);
//            }
//        }
//        return result;
//    }

    public BitSet getBitsetValue(Word<I> word) {
        if (allRows.containsKey(word)) {
            return ((RecursiveContent) allRows.get(word).getContent()).getContentVal(0);
        }
        BitSet T = new BitSet();
        for(int contIdx= 0; contIdx< contextPairs.size(); contIdx++) {
            if (learner.recursiveMembershipQuery(
                    contextPairs.get(contIdx).getFirst(),
                    callSymbol,
                    word,
                    returnSymbol,
                    contextPairs.get(contIdx).getSecond()
            )) {
                T.set(contIdx);
            }
        }
        return T;
    }

    public Word<I> getRecursiveEquivalent(BitSet T) {
        for (Map.Entry<Word<I>, BitSet> recEquivClass: recursiveEquivalenceClasses.entrySet()){
            if (recEquivClass.getValue().equals(T))
                return recEquivClass.getKey();
        }
        return null;
    }

    public boolean isAccepted(Word<I> word, Word<I> recEquivClass) {
        Word<I> equivalent = getTransitions(
                (SeparateRecursiveRow<I>) allRows.get(Word.epsilon()),
                word,
                recEquivClass);
        return areRecursiveEquivalent(recursiveEquivalenceClasses.get(recEquivClass), equivalent, 0);
    }

    public Word<I> getTransitions(SeparateRecursiveRow<I> row, Word<I> toRead, Word<I> recEquivClass) {
        if (row == null) {
            return null;
        }
        if (toRead.isEmpty()) {
            return row.getPrefix();
        }
        return getTransitions(
                row.getSuccessor(inputAlphabet.getSymbolIndex(toRead.firstSymbol()), recEquivClass),
                toRead.suffix(toRead.size()-1),
                recEquivClass
        );
    }

    public List<Pair<Word<I>, Word<I>>> getContextPairs(){
        return contextPairs;
    }

    public boolean isInRows(Word<I> word) {
        return allRows.containsKey(word);
    }

    public Set<Word<I>> getRecursiveEquivalenceClasses() {
        return recursiveEquivalenceClasses.keySet();
    }

    public Word<I> getRegularEquivalent(Word<I> input, Word<I> recEquivClass) {
        return getTransitions(
                (SeparateRecursiveRow<I>) allRows.get(Word.epsilon()),
                input,
                recEquivClass
        );
    }
}
