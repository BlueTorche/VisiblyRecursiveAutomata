package learner.VRA;

import learner.ObservationTable;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.*;
import java.util.stream.Collectors;

public class RecursiveObservationTable<I> implements ObservationTable<I> {
    GrowingAlphabet<I> regularAlphabet;
    I callSymbol;
    I returnSymbol;
    VRALearner<I> learner;

    Set<Pair<Word<I>, Word<I>>> context = new HashSet<>();
    List<Word<I>> representatives = new ArrayList<>();
    List<Word<I>> separators = new ArrayList<>();
    Map<Word<I>, Set<Pair<Word<I>, Word<I>>>> table = new HashMap<>();
    Map<Long,Word<I>> equivalenceClasses = new HashMap<>();

    public RecursiveObservationTable(GrowingAlphabet<I> regularAlphabet, I callSymbol, I returnSymbol, VRALearner<I> learner) {
        this.regularAlphabet = regularAlphabet;
        this.callSymbol = callSymbol;
        this.returnSymbol = returnSymbol;
        this.learner = learner;
    }

    @Override
    public void addRepresentative(Word<I> r) {
        if (representatives.contains(r)) {
            return;
        }
        representatives.add(r);
        for (Word<I> s : separators) {
            fillTableCase(Word.fromWords(r, s));
        }
        Long equivClass = getRegularEquivalenceClass(r);
        if (equivClass != null && !equivalenceClasses.containsKey(equivClass)) {
            equivalenceClasses.put(equivClass, r);
        }

        for (I i : regularAlphabet) {
            for (Word<I> s : separators) {
                fillTableCase(Word.fromWords(r, Word.fromLetter(i), s));
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

        separators.add(s);
        for (Word<I> r : representatives) {
            fillTableCase(Word.fromWords(r, s));
            for (I i : regularAlphabet) {
                fillTableCase(Word.fromWords(r, Word.fromLetter(i), s));
            }
        }

        computeEquivalenceClasses();


        if (!s.isEmpty()) {
            addSeparator(s.suffix(s.size()-1));
        }
    }

    @Override
    public void closeTable() {
        Word<I> newRepresentative = null;
        for (Word<I> r : representatives) {
            for (I i  : regularAlphabet) {
                Word<I> ri = Word.fromWords(r, Word.fromLetter(i));
                Long equivClass = getRegularEquivalenceClass(ri);
                if (equivClass != null && !equivalenceClasses.containsKey(equivClass)) {
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

    public boolean makeTablePConsistent() {
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
    public void makeTableClosedConsistent() {
        int iter = 0;
        do {
            do {
                closeTable();
                System.out.println("Rec Table: " + table);
                System.out.println("\t representatives : " + representatives);
                System.out.println("\t separators : " + separators);
                System.out.println("Reg Equiv Classes: " + equivalenceClasses);
                System.out.println("Context: " + context);
                iter++;
                if (iter > 5) throw new RuntimeException("Too many iterations");
            } while (makeTableSigmaConsistent());
        } while(makeTablePConsistent());
    }

    @Override
    public void processCounterExample(Word<I> cx) {
        addRepresentative(cx);
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
    }

    public void addContext(Word<I> prefix, Word<I> suffix){
        Pair<Word<I>, Word<I>> pair = Pair.of(prefix, suffix);
        context.add(pair);

        for (Word<I> r: representatives) {
            for(Word<I> s: separators) {
                Word<I> word = Word.fromWords(r, s);
                if(learner.recursiveMembershipQuery(pair.getFirst(), callSymbol, word, returnSymbol, pair.getSecond())) {
                    table.get(word).add(pair);
                }

                for(I i: regularAlphabet) {
                    word = Word.fromWords(r, Word.fromLetter(i), s);
                    if(learner.recursiveMembershipQuery(pair.getFirst(), callSymbol, word, returnSymbol, pair.getSecond())) {
                        table.get(word).add(pair);
                    }
                }
            }
        }

        computeEquivalenceClasses();
    }

    public void addSymbol(I symbol) {
        regularAlphabet.add(symbol);

        for (Word<I> r: representatives) {
            for(Word<I> s: separators) {
                fillTableCase(Word.fromWords(r, Word.fromLetter(symbol), s));
            }
        }

        computeEquivalenceClasses();
    }

    public void fillTableCase(Word<I> word) {
        if (!table.containsKey(word)) {
            table.put(word, new HashSet<>());
        }
        for(Pair<Word<I>, Word<I>> pair : context) {
            if(learner.recursiveMembershipQuery(pair.getFirst(), callSymbol, word, returnSymbol, pair.getSecond())) {
                table.get(word).add(pair);
            }
        }
    }

    public Map<Word<I>, FastDFA<I>> constructAllHypotheses() {
        List<Word<I>> recEquivClasses = learner.getRecursiveEquivalenceClasses(callSymbol, returnSymbol);
        Map<Word<I>, FastDFA<I>> hypotheses = new HashMap<>();

        printTable();

        for (Word<I> recEquivClass : recEquivClasses) {
            Set<Pair<Word<I>,Word<I>>> contextEquivClass = table.get(recEquivClass.subWord(1, recEquivClass.size()-1));

            FastDFA<I> hypothesis = new FastDFA<>(regularAlphabet);
            HashMap<Word<I>, Integer> classToID = new HashMap<>();

            for(Word<I> eq : equivalenceClasses.values()) {
                FastDFAState s = hypothesis.addState(table.get(eq).equals(contextEquivClass));
                classToID.put(eq, s.getId());
                if (eq.equals(Word.epsilon())) {
                    hypothesis.setInitial(s, true);
                }
            }
            for(Word<I> eq : equivalenceClasses.values()) {
                FastDFAState s = hypothesis.getState(classToID.get(eq));
                System.out.println(classToID);
                for(I i: regularAlphabet) {
//                    System.out.println(Word.fromWords(eq, Word.fromLetter(i)) + ": " +
//                            getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i))) + " "
//                            + equivalenceClasses.get(
//                            getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i)))
//                    ));
                    hypothesis.addTransition(s, i,
                            hypothesis.getState(classToID.get(
                                    equivalenceClasses.get(
                                            getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i)))
                                    )
                            ))
                    );
                }
            }
            hypotheses.put(recEquivClass, hypothesis);
        }
        return hypotheses;
    }

    public Long getRegularEquivalenceClass(Word<I> r) {
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

    public Word<I> getRecursiveEquivalent(Word<I> rs) {
        for (Word<I> recEquivClass: learner.getRecursiveEquivalenceClasses(callSymbol, returnSymbol)) {
            if (table.get(recEquivClass.subWord(1, recEquivClass.size()-1)).equals(table.get(rs))) {
                return recEquivClass;
            }
        }
        return null;
    }

    private void computeEquivalenceClasses() {
        equivalenceClasses = new HashMap<>();
        for (Word<I> r : representatives) {
            Long equivClass = getRegularEquivalenceClass(r);
            System.out.println("Equiv Class: " + r + " -- " + equivClass);
            if (equivClass != null && !equivalenceClasses.containsKey(equivClass)) {
                equivalenceClasses.put(equivClass, r);
            }
        }
    }

    public Map<Word<I>, FastDFA<I>> constructReducedHypotheses() {
        List<Word<I>> recEquivClasses = learner.getRecursiveEquivalenceClasses(callSymbol, returnSymbol);
        Map<Word<I>, FastDFA<I>> hypotheses = new HashMap<>();

        printTable();

        for (Word<I> recEquivClass : recEquivClasses) {
            Set<Pair<Word<I>,Word<I>>> contextEquivClass = table.get(recEquivClass.subWord(1, recEquivClass.size()-1));
            Map<Word<I>, Word<I>> reducedEquivClass = getReducedEquivClasses(contextEquivClass);

            System.out.println(reducedEquivClass);

            FastDFA<I> hypothesis = new FastDFA<>(regularAlphabet);
            HashMap<Word<I>, Integer> classToID = new HashMap<>();

            for(Word<I> eq : new HashSet<>(reducedEquivClass.values())) {
                FastDFAState s = hypothesis.addState(table.get(eq).equals(contextEquivClass));
                classToID.put(eq, s.getId());
                if (eq.equals(Word.epsilon())) {
                    hypothesis.setInitial(s, true);
                }
            }
            for(Word<I> eq : new HashSet<>(reducedEquivClass.values())) {
                FastDFAState s = hypothesis.getState(classToID.get(eq));
                for(I i: regularAlphabet) {
//                    System.out.println(Word.fromWords(eq, Word.fromLetter(i)) + ": " +
//                            getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i))) + " "
//                            + reducedEquivClass.get(equivalenceClasses.get(
//                                getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i)))
//                    ))
//                            + " " + equivalenceClasses.get(
//                                getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i))))
//                    );
                    hypothesis.addTransition(s, i,
                            hypothesis.getState(classToID.get(
                                    reducedEquivClass.get(
                                        equivalenceClasses.get(
                                                getRegularEquivalenceClass(Word.fromWords(eq, Word.fromLetter(i)))
                                        )
                                    )
                            ))
                    );
                }
            }
            hypotheses.put(recEquivClass, hypothesis);
        }
        return hypotheses;
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
            for(I symbol : regularAlphabet) {
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
