package learner.VRA;

import java.util.*;

import learner.ObservationTable.ObservationTable;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;


public class RegularObservationTable<I> implements ObservationTable<I> {
    GrowingAlphabet<I> alphabet;
    VRALearner<I> learner;

    List<Word<I>> representatives = new ArrayList<>();
    List<Word<I>> separators = new ArrayList<>();
    Map<Word<I>, Boolean> table = new HashMap<>();
    Map<Integer, Word<I>> equivalenceClasses = new HashMap<>();

    public RegularObservationTable(GrowingAlphabet<I> alphabet, VRALearner<I> learner) {
        this.alphabet = alphabet;
        this.learner = learner;
    }

    @Override
    public void addRepresentative(Word<I> r) {
        if (representatives.contains(r)) {
            return;
        }
        representatives.add(r);
        for (Word<I> s : separators) {
            Word<I> rs = Word.fromWords(r, s);
            if (!table.containsKey(rs)) {
                table.put(rs, learner.askMembershipQuery(rs));
            }
        }

        if(equivalenceClasses != null) {
            Integer equivalenceClass = getEquivalenceClassKey(r);
            if (!equivalenceClasses.containsKey(equivalenceClass)) {
                equivalenceClasses.put(equivalenceClass, r);
            }
        }

        for (I i : alphabet) {
            Word<I> ri = Word.fromWords(r, Word.fromLetter(i));
            for (Word<I> s : separators) {
                Word<I> ris = Word.fromWords(ri, s);
                if (!table.containsKey(ris)) {
                    table.put(ris, learner.askMembershipQuery(ris));
                }
            }
        }
        if (!r.isEmpty()) {
            addRepresentative(r.prefix(r.size()-1));
        }
    }

    @Override
    public void addSeparator(Word<I> s) {
        if (separators.contains(s)) {
            return;
        }
        equivalenceClasses = new HashMap<>();
        separators.add(s);
        for (Word<I> r : representatives) {
            Word<I> rs = Word.fromWords(r, s);
            if (!table.containsKey(rs)) {
                table.put(rs, learner.askMembershipQuery(rs));
            }
            Integer equivalenceClass = getEquivalenceClassKey(r);
            if (!equivalenceClasses.containsKey(equivalenceClass)) {
                equivalenceClasses.put(equivalenceClass, r);
            }

            for (I i : alphabet) {
                Word<I> ris = Word.fromWords(r, Word.fromLetter(i), s);
                if (!table.containsKey(ris)) {
                    table.put(ris, learner.askMembershipQuery(ris));
                }
            }
        }
        if (!s.isEmpty()) {
            addSeparator(s.suffix(s.size()-1));
        }
    }

    @Override
    public boolean close() {
        Word<I> newRepresentative = null;
        for (Word<I> r : representatives) {
            for (I i  : alphabet) {
                Word<I> ri = Word.fromWords(r, Word.fromLetter(i));
                if (!equivalenceClasses.containsKey(getEquivalenceClassKey(ri))) {
                    newRepresentative = ri;
                    break;
                }
            }
        }
        if (newRepresentative != null) {
            addRepresentative(newRepresentative);
            close();
        }
        return false;
    }

    @Override
    public boolean consistent() {
        boolean result = false;
        for (int i =  0; i < representatives.size(); i++) {
            for (int j = i + 1; j < representatives.size(); j++) {
                Word<I> r1 = representatives.get(i);
                Word<I> r2 = representatives.get(j);
                if (isEquivalent(r1, r2)) {
                    for (I a: alphabet) {
                        Word<I> r1a = Word.fromWords(r1, Word.fromLetter(a));
                        Word<I> r2a = Word.fromWords(r2, Word.fromLetter(a));
                        for (Word<I> s : separators) {
                            if (table.get(Word.fromWords(r1a, s)) != table.get(Word.fromWords(r2a, s))) {
                                addSeparator(Word.fromWords(Word.fromLetter(a), s));
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void enforce() {
        do {
            close();
        } while (consistent());
    }

    public FastDFA<I> constructHypothesis() {
        printTable();

        FastDFA<I> hypothesis = new FastDFA<>(alphabet);
        HashMap<Word<I>, Integer> classToID = new HashMap<>();
        for(Word<I> eq : equivalenceClasses.values()) {
            FastDFAState s = hypothesis.addState(table.get(eq));
            classToID.put(eq, s.getId());
            if (eq.equals(Word.epsilon())) {
                hypothesis.setInitial(s, true);
            }
        }
        for(Word<I> eq : equivalenceClasses.values()) {
            FastDFAState s = hypothesis.getState(classToID.get(eq));
            for(I i: alphabet) {
                hypothesis.addTransition(s, i,
                        hypothesis.getState(classToID.get(
                                equivalenceClasses.get(
                                        getEquivalenceClassKey(Word.fromWords(eq, Word.fromLetter(i)))
                                )
                        ))
                );
            }
        }

        return hypothesis;
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
        enforce();
    }

    public void addSymbol(I symbol) {
        alphabet.add(symbol);

        for (Word<I> r: representatives) {
            for(Word<I> s: separators) {
                Word<I> word = Word.fromWords(r, Word.fromLetter(symbol), s);
                table.put(word, learner.askMembershipQuery(word));
            }
        }
    }

    private boolean isEquivalent(Word<I> r1, Word<I> r2) {
        return getEquivalenceClassKey(r1).equals(getEquivalenceClassKey(r2));
    }

    private Integer getEquivalenceClassKey(Word<I> r) {
        int bs = 0;
        for (int i = 0; i < separators.size(); i++) {
            bs += table.get(Word.fromWords(r, separators.get(i))) ? (int) Math.pow(2, i) : 0;
        }
        return bs;
    }

    private void printTable() {
        System.out.println("\t" + separators);
        for(Word<I> r: representatives) {
            System.out.print(r + "\t| ");
            for(Word<I> s: separators) {
                System.out.print(table.get(Word.fromWords(r, s)) + "\t");
            }
            System.out.println();
        }
        System.out.println("------------------------");
        for(Word<I> r: representatives) {
            for(I symbol : alphabet) {
                Word<I> ri = Word.fromWords(r, Word.fromLetter(symbol));
                System.out.print(ri + "\t| ");
                for (Word<I> s : separators) {
                    System.out.print(table.get(Word.fromWords(ri, s)) + "\t");
                }
                System.out.println();
            }
        }
        System.out.println(equivalenceClasses);
    }
}
