package learner.ObservationTable;

import learner.Learner;
import learner.ObservationTable.Row.RegularRow;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.Automaton;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

import java.util.*;

public abstract class AbstractObservationTable<I> implements ObservationTable<I> {
    protected List<Word<I>> representatives = new ArrayList<>();
    protected Set<Word<I>> primeRepresentatives = new HashSet<>();

    protected List<Word<I>> separators = new ArrayList<>();

    protected Map<Word<I>, RegularRow<I>> allRows = new HashMap<>();

    protected GrowingAlphabet<I> inputAlphabet;
    protected Learner<I, ?> learner;

    @Override
    public void addRepresentative(Word<I> r) {
        if (representatives.contains(r))
            return;
        if (!r.isEmpty()) {
            addRepresentative(r.prefix(r.size()-1));
        }
        representatives.add(r);
        RegularRow<I> row = createNewRow(r);
        for(int idx = 0; idx < inputAlphabet.size(); idx++) {
            row.setSuccessorRow(idx, createNewRow(Word.fromWords(r, Word.fromLetter(inputAlphabet.getSymbol(idx)))));
        }
    }

    @Override
    public void addSeparator(Word<I> s) {
        if (!separators.contains(s)) {
            if (!s.isEmpty()) {
                addSeparator(s.suffix(s.size()-1));
            }
            separators.add(s);
            createNewColumn(separators.size()-1);
        }
    }

    @Override
    public boolean close() {
        boolean toRet = false;
        for (Word<I> r: primeRepresentatives) {
            if (!representatives.contains(r)) {
                addRepresentative(r);
                toRet = true;
            }
        }
        return toRet;
    }

    @Override
    public boolean consistent() {
        boolean toRet = false;
        for (Word<I> r: representatives) {
            RegularRow<I> row = allRows.get(r);
            if (!row.isPrime()) {
                Word<I> separator = getInconsistentSeparator(row, (RegularRow<I>) row.getParent());
                if (separator != null) {
                    System.out.println(r + " -- " + row.getParent().getPrefix() + " -- " + separator);
                    addSeparator(separator);
                    toRet = true;
                }
            }
        }
        return toRet;
    }

    @Override
    public void enforce() {
        int i = 0;
        while(close() || consistent()) {
            System.out.println(close());
            System.out.println(consistent());
            System.out.println(primeRepresentatives);
            i++;
            if (i>10){
                break;
            }
        }
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
    }

    private RegularRow<I> createNewRow(Word<I> r){
        if (!allRows.containsKey(r)) {
            allRows.put(r, new RegularRow<>(r));
            for (int idx = 0; idx < separators.size(); idx++) {
                fetchMembership(allRows.get(r), idx);
            }
            checkRowPrime(allRows.get(r));
        }
        return allRows.get(r);
    }

    private void createNewColumn(int idx) {
        for(RegularRow<I> row: allRows.values()) {
            fetchMembership(row, idx);
        }
        checkNewColumnPrimes();
    }

    private void fetchMembership(RegularRow<I> row, int idx) {
        if (learner.askMembershipQuery(
                Word.fromWords(row.getPrefix(), separators.get(idx))
        )) {
            row.fetchContent(idx);
        }
    }

    private void checkRowPrime(RegularRow<I> row) {
        for (Word<I> prime: primeRepresentatives) {
            if (areEquivalent(allRows.get(prime), row)){
                System.out.println("Equivalent: " + prime + "-" + row.getPrefix());
                return;
            }
        }
        row.setParent(null);
        primeRepresentatives.add(row.getPrefix());
    }

    private void checkNewColumnPrimes() {
        for (Word<I> r: representatives) {
            RegularRow<I> row = allRows.get(r);
            if (!row.isPrime() && !areEquivalent((RegularRow<I>) row.getParent(), row)) {
                checkRowPrime(row);
            }
        }
        for (RegularRow<I> row: allRows.values()) {
            if (!representatives.contains(row.getPrefix())) {
                if (!row.isPrime() && !areEquivalent((RegularRow<I>) row.getParent(), row)) {
                    checkRowPrime(row);
                }
            }
        }
    }

    public void addSymbol(I symbol) {
        inputAlphabet.add(symbol);
        for (Word<I> r: representatives){
            allRows.get(r).setSuccessorRow(inputAlphabet.size() -1,
                    createNewRow(Word.fromWords(r, Word.fromLetter(symbol))));
        }
    }

    private boolean areEquivalent(RegularRow<I> prime, RegularRow<I> row){
        if(row.equivalentTo(prime)) {
            row.setParent(prime);
            return true;
        }
        return false;
    }

    private Word<I> getInconsistentSeparator(RegularRow<I> row1, RegularRow<I> row2) {
        int symbolIdx = row1.consistentTo(row2);
        if (symbolIdx == -1)
            return null;
        int separatorIdx = row1.getSuccessor(symbolIdx).getDistinctionSeparator(row2.getSuccessor(symbolIdx));
        assert separatorIdx != -1;
        return Word.fromWords(Word.fromLetter(inputAlphabet.getSymbol(symbolIdx)), separators.get(separatorIdx));
    }

    @Override
    public Automaton<?, I, ?> constructHypothesis() {
        FastDFA<I> hypothesis = new FastDFA<>(inputAlphabet);
        HashMap<RegularRow<I>, Integer> rowToStateID = new HashMap<>();
        for(Word<I> eqClass : primeRepresentatives) {
            FastDFAState s = hypothesis.addState(allRows.get(eqClass).isAccepting());
            rowToStateID.put(allRows.get(eqClass), s.getId());
            if (eqClass.equals(Word.epsilon())) {
                hypothesis.setInitial(s, true);
            }
        }
        for(Word<I> eqClass : primeRepresentatives) {
            for (int idx = 0; idx < inputAlphabet.size(); idx++) {
                RegularRow<I> ingoingRow = (RegularRow<I>) allRows.get(eqClass).getSuccessor(idx);
                RegularRow<I> primeIngoingRow = ingoingRow.isPrime() ? ingoingRow : (RegularRow<I>) ingoingRow.getParent();
                hypothesis.addTransition(
                        hypothesis.getState(rowToStateID.get(allRows.get(eqClass))),
                        inputAlphabet.getSymbol(idx),
                        hypothesis.getState((rowToStateID.get(primeIngoingRow)))
                );
            }
        }

        return hypothesis;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("\t  " + separators.toString());
        for(Word<I> r: representatives) {
            output.append("\n").append(allRows.get(r));
        }
        output.append("\n----------------------");
        for(Word<I> r: allRows.keySet()){
            if (!representatives.contains(r)) {
                output.append("\n").append(allRows.get(r));
            }
        }
        return output.toString();
    }
}
