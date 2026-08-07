package learner.VRA.seperateLearning;

import learner.VRA.VRALearner;
import learner.VRA.isomophicLearning.RecursiveObservationTable;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeperateRecursiveObservationTable<I> extends RecursiveObservationTable<I> {
    public SeperateRecursiveObservationTable(GrowingAlphabet<I> regularAlphabet, I callSymbol, I returnSymbol, VRALearner<I> learner) {
        super(regularAlphabet, callSymbol, returnSymbol, learner);
    }


    @Override
    public boolean close() { //todo
        for (Word<I> r : representatives) {
            for (I i  : regularAlphabet) {
                Word<I> ri = Word.fromWords(r, Word.fromLetter(i));
                Long equivClass = getRegularEquivalenceClass(ri);
                if (equivClass != null && !equivalenceClasses.containsKey(equivClass)) {
                    addRepresentative(ri);
                    close();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean consistent() { //todo
        for (int i =  0; i < representatives.size(); i++) {
            Word<I> r1 = representatives.get(i);
            Long equivClass1 = getRegularEquivalenceClass(r1);
            if (equivClass1 != null) {
                for (int j = i + 1; j < representatives.size(); j++) {
                    Word<I> r2 = representatives.get(j);
                    if (equivClass1.equals(getRegularEquivalenceClass(r2))) {
                        for (I a: regularAlphabet) {
                            Word<I> r1a = Word.fromWords(r1, Word.fromLetter(a));
                            Word<I> r2a = Word.fromWords(r2, Word.fromLetter(a));
                            for (Word<I> s : separators) {
                                if (!table.get(Word.fromWords(r1a, s)).equals(table.get(Word.fromWords(r2a, s)))) {
                                    addSeparator(Word.fromWords(Word.fromLetter(a), s));
                                    System.out.println("Added separator " + Word.fromWords(Word.fromLetter(a), s) +
                                            "\n\tSigma inconsitence of " + r1a + " and " + r2a);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean makeTableProcComplete() {
        for (Word<I> r : representatives) {
            for (Word<I> s : separators) {
                Word<I> word =  Word.fromWords(r, s);
                if (getRecursiveEquivalent(word) == null) {
                    learner.addProceduralSymbol(word, callSymbol, returnSymbol);
                    addRepresentative(word);
                    return true;
                }
            }
            for (I i : regularAlphabet) {
                for (Word<I> s : separators) {
                    Word<I> word =  Word.fromWords(r, Word.fromLetter(i), s);
                    if (getRecursiveEquivalent(word) == null) {
                        learner.addProceduralSymbol(word, callSymbol, returnSymbol);
                        addRepresentative(word);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public Long getRegularEquivalenceClass(Word<I> r) { // todo
        List<Word<I>> recEquivClasses = learner.getRecursiveEquivalenceClasses(callSymbol, returnSymbol);
        int base = recEquivClasses.size();
        long value = 0;
        for (int i = 0; i < separators.size(); i++) {
            Word<I> s = separators.get(i);
            boolean found = false;
            for (Word<I> recEquivClass : recEquivClasses) {
                Word<I> word = recEquivClass.subWord(1, recEquivClass.size()-1);
                if (table.get(Word.fromWords(r, s)).equals(table.get(word))) {
                    value += ((long) (Math.pow(base, i))*learner.getIndexOfRecursiveEquivalenceClass(recEquivClass));
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        return value;
    }

    private Map<Word<I>, Word<I>> getReducedEquivClasses(Set<Pair<Word<I>,Word<I>>> context) {
        Map<Word<I>, Word<I>> EquivClass = new HashMap<>();
        List<Word<I>> equivClass = equivalenceClasses.values().stream().toList();
        for (int i = 0; i < equivalenceClasses.size(); i++) {
            Word<I> r1 = equivClass.get(i);
            Word<I> equivalent = r1;
            for (int j = equivalenceClasses.size()-1; j > i; j--) {
                Word<I> r2 = equivClass.get(j);
                boolean areEquivalent = true;
                for (Word<I> s: separators) {
                    boolean ac1 = table.get(Word.fromWords(r1, s)).equals(context);
                    boolean ac2 = table.get(Word.fromWords(r2, s)).equals(context);
                    if (!(ac1 && ac2 || !ac1 && !ac2)) {
                        areEquivalent = false;
                        break;
                    }
                    for (I symbol: regularAlphabet) {
                        ac1 = table.get(Word.fromWords(r1, Word.fromLetter(symbol), s)).equals(context);
                        ac2 = table.get(Word.fromWords(r2, Word.fromLetter(symbol), s)).equals(context);
                        if (!(ac1 && ac2 || !ac1 && !ac2)) {
                            areEquivalent = false;
                            break;
                        }
                    }
                    if (!areEquivalent) { break; }
                }

                if (areEquivalent) {
                    equivalent = r2;
                    break;
                }
            }
            EquivClass.put(r1, equivalent);
        }
        return EquivClass;
    }
}
