package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.Row.SeparateRecursiveRow;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.*;

public class SeparateRecursiveObservationTableOptimized<I> extends SeparateRecursiveObservationTable<I> {
    public SeparateRecursiveObservationTableOptimized(GrowingAlphabet<I> inputAlphabet, AbstractVRALearner<I> learner, I callSymbol, I returnSymbol) {
        super(inputAlphabet, learner, callSymbol, returnSymbol);
    }

    public Set<Word<I>> getAllEquivalent(Word<I> input) {
        if (allRows.containsKey(input)) {
            return Collections.singleton(getRecursiveEquivalent(input));
        }
        Set<Word<I>> result = new HashSet<>();
        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
            Word<I> equivalent = getTransitions(
                    (SeparateRecursiveRow<I>) allRows.get(Word.epsilon()),
                    input,
                    recEquivClass);
            if (equivalent != null) {
                result.add(getRecursiveEquivalent(equivalent));
            }
        }
        return result;
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
}
