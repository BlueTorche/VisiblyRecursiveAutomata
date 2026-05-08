package learner.DFA;

import learner.Learner;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;
import learner.ObservationTable;
import oracle.Oracle;

import java.util.*;

public class AbstractLStar<I> implements ObservationTable<I>, Learner<I, FastDFA<I>> {
    Alphabet<I> alphabet;
    Oracle<I, DFA<?, I>> oracle;

    List<Word<I>> representatives = new ArrayList<>();
    List<Word<I>> separators = new ArrayList<>();
    Map<Word<I>, Boolean> table = new HashMap<>();
    Map<Integer, Word<I>> equivalenceClasses = new HashMap<>();

    public AbstractLStar(Alphabet<I> alphabet, Oracle<I, DFA<?, I>> oracle) {
        this.alphabet = alphabet;
        this.oracle = oracle;
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
                table.put(rs, oracle.MembershipQuery(rs));
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
                    table.put(ris, oracle.MembershipQuery(ris));
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
                table.put(rs, oracle.MembershipQuery(rs));
            }

            Integer equivalenceClass = getEquivalenceClassKey(r);
            if (!equivalenceClasses.containsKey(equivalenceClass)) {
                equivalenceClasses.put(equivalenceClass, r);
            }

            for (I i : alphabet) {
                Word<I> ris = Word.fromWords(r, Word.fromLetter(i), s);
                if (!table.containsKey(ris)) {
                    table.put(ris, oracle.MembershipQuery(ris));
                }
            }
        }
        if (!s.isEmpty()) {
            addSeparator(s.suffix(s.size()-1));
        }
    }

    @Override
    public void closeTable() {
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
            closeTable();
        }
    }

    @Override
    public boolean makeTableSigmaConsistent() {
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
    public void makeTableClosedConsistent() {
        do {
            closeTable();
        } while (makeTableSigmaConsistent());
    }

    @Override
    public FastDFA<I> constructHypothesis() {
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
    public void processCounterExample(Word<I> cx) {
        addRepresentative(cx);
    }

    @Override
    public FastDFA<I> learn() {
        initialize();
        for(int i = 0; i < 10; i++) {
            makeTableClosedConsistent();
            printTable();

            FastDFA<I> hypothesis = constructHypothesis();
            Word<I> cx = oracle.EquivalenceQuery(hypothesis);
            if (cx == null){
                return hypothesis;
            } else {
                processCounterExample(cx);
            }
            System.out.println("Processed Couterexample:" + cx);
        }
        return constructHypothesis();
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
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
