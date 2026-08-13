package learner.ObservationTable.RecursiveObservationTable;

import learner.Learner;
import learner.ObservationTable.Row.RecursiveContent;
import learner.ObservationTable.Row.RegularRow;
import learner.ObservationTable.Row.Row;
import learner.ObservationTable.Row.SeparateRecursiveRow;
import learner.VRA.VRALearner;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

import java.util.*;

public class SeparateRecursiveObservationTable<I> extends AbstractRecursiveObservationTable<I> {
    Map<Word<I>, Set<Word<I>>> primeContextRepresentatives = new HashMap<>();

    public SeparateRecursiveObservationTable(GrowingAlphabet<I> inputAlphabet,
                                             VRALearner<I> learner,
                                             I callSymbol,
                                             I returnSymbol) {
        this.inputAlphabet = inputAlphabet;
        this.learner = learner;
        this.callSymbol = callSymbol;
        this.returnSymbol = returnSymbol;
    }

    @Override
    public FastDFA<I> constructIndividualHypothesis(Word<I> recEquivClass) {
//        BitSet context =((SeparateRecursiveRow<I>) allRows.get(recEquivClass)).getBaseContext();

        System.out.println(this);
        System.out.println();
        System.out.println(primeContextRepresentatives.get(recEquivClass));
        System.out.println();
        System.out.println(primeRepresentatives);
        System.out.println("---------------");

        FastDFA<I> hypothesis = new FastDFA<>(inputAlphabet);
        HashMap<Row<I>, Integer> rowToStateID = new HashMap<>();
        for(Word<I> eqClass : primeContextRepresentatives.get(recEquivClass)) {
            FastDFAState s = hypothesis.addState(areRecursiveEquivalent(recEquivClass, eqClass, 0));
            rowToStateID.put(allRows.get(eqClass), s.getId());
            if (eqClass.equals(Word.epsilon())) {
                hypothesis.setInitial(s, true);
            }
        }
        for(Word<I> eqClass : primeContextRepresentatives.get(recEquivClass)) {
            for (int idx = 0; idx < inputAlphabet.size(); idx++) {
                SeparateRecursiveRow<I> ingoingRow = (SeparateRecursiveRow<I>) allRows.get(eqClass).getSuccessor(idx);
                SeparateRecursiveRow<I> primeIngoingRow = (
                        ingoingRow.isContextPrime(recEquivClass) ? ingoingRow : ingoingRow.getContextParent(recEquivClass)
                );

//                System.out.println(allRows.get(eqClass));
//                System.out.println(ingoingRow.isContextPrime(context));

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
    public boolean procComplete() {
        for (Word<I> r: representatives) {
            if (checkAndFetchRecursivePrime(r, 0)) {
                return true;
            }
        }
        for (Word<I> r: allRows.keySet()) {
            if (representatives.contains(r)) {
                continue;
            }
            for (int i =0; i < separators.size(); i++) {
                if (checkAndFetchRecursivePrime(r, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAndFetchRecursivePrime(Word<I> rep, int sepIdx) {
        if (isRecursivePrime(rep, sepIdx)) {
            Word<I> recEquivClass = Word.fromWords(rep, separators.get(sepIdx));
            recursiveEquivalenceClasses.add(recEquivClass);
            primeContextRepresentatives.put(recEquivClass, new HashSet<>());
            primeContextRepresentatives.get(recEquivClass).add(recEquivClass);
            primeRepresentatives.add(recEquivClass);
            checkNewPrimes(recEquivClass);
            ((VRALearner<I>) learner).addProceduralSymbol(recEquivClass, callSymbol, returnSymbol);
            System.out.println("Non procedural completed. Added symbol " + callSymbol + recEquivClass + returnSymbol);
            return true;
        }
        return false;
    }

    @Override
    public Row<I> createNewEmptyRow(Word<I> prefix) {
        return new SeparateRecursiveRow<>(prefix, separators.size());
    }

    @Override
    public void checkRowPrime(Row<I> row) {
        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
            checkRowPrime((SeparateRecursiveRow<I>) row, recEquivClass);
        }
    }

    public void checkRowPrime(SeparateRecursiveRow<I> row, Word<I> recEquivClass) {
        BitSet context = ((SeparateRecursiveRow<I>) allRows.get(recEquivClass)).getBaseContext();
        for (Word<I> prime: primeContextRepresentatives.get(recEquivClass)) {
            SeparateRecursiveRow<I> primeRow = (SeparateRecursiveRow<I>) allRows.get(prime);
//            System.out.println(primeRow + " -- " + row + " -- " + areEquivalent(primeRow, row, context));
            if (!primeRow.equals(row) && areEquivalent(primeRow, row, context)) {
                row.setContextParent(primeRow, recEquivClass);
                return;
            }
        }
//        System.out.println(this);
//        System.out.println(primeContextRepresentatives.get(recEquivClass));
//        System.out.println("New prime detected for " + recEquivClass + ": " + row);
        row.setContextPrime(recEquivClass);
        primeContextRepresentatives.get(recEquivClass).add(row.getPrefix());
        primeRepresentatives.add(row.getPrefix());
    }

    @Override
    public void checkNewPrimes() {
        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
            BitSet context = ((SeparateRecursiveRow<I>) allRows.get(recEquivClass)).getBaseContext();
            for (Word<I> r: representatives) {
                SeparateRecursiveRow<I> row = (SeparateRecursiveRow<I>) allRows.get(r);
                if (!row.isContextPrime(recEquivClass) && !areEquivalent(row.getContextParent(recEquivClass), row, context)) {
                    checkRowPrime(row, recEquivClass);
                }
            }
            for (Row<I> row: allRows.values()) {
                if (!representatives.contains(row.getPrefix())) {
                    if (!((SeparateRecursiveRow<I>) row).isContextPrime(recEquivClass)
                            && !areEquivalent(((SeparateRecursiveRow<I>) row).getContextParent(recEquivClass), row, context)) {
                        checkRowPrime(((SeparateRecursiveRow<I>) row), recEquivClass);
                    }
                }
            }
        }
    }

    private void checkNewPrimes(Word<I> recEquivClass) {
        for (Word<I> r: representatives) {
            SeparateRecursiveRow<I> row = (SeparateRecursiveRow<I>) allRows.get(r);
            checkRowPrime(row, recEquivClass);
        }
        for (Row<I> row: allRows.values()) {
            if (!representatives.contains(row.getPrefix())) {
                checkRowPrime(((SeparateRecursiveRow<I>) row), recEquivClass);
            }
        }
    }

    @Override
    public boolean consistent() {
        boolean toRet = false;
        for (Word<I> recPrime: recursiveEquivalenceClasses) {
            BitSet context = ((SeparateRecursiveRow<I>) allRows.get(recPrime)).getBaseContext();
            for (Word<I> r : representatives) {
                SeparateRecursiveRow<I> row = (SeparateRecursiveRow<I>) allRows.get(r);
                if (!row.isContextPrime(recPrime)) {
                    Word<I> separator = getInconsistentSeparator(row, row.getContextParent(recPrime), context);
                    if (separator != null) {
                        addSeparator(separator);
//                        System.out.println(r + " -- " + row.getContextParent(recPrime).getPrefix() + " -- " + separator + " -- " + context);
                        System.out.println("Non consistent. Added separator " + separator);
                        toRet = true;
                    }
                }
            }
        }
        return toRet;
    }

    @Override
    public Word<I> getInconsistentSeparator(Row<I> row1, Row<I> row2) {
        return null; // Nothing to do, this is not supposed to be call in this class
    }

    public Word<I> getInconsistentSeparator(SeparateRecursiveRow<I> row1, SeparateRecursiveRow<I> row2, BitSet context) {
        int symbolIdx = row1.contextConsistentTo(row2, context);
        if (symbolIdx == -1)
            return null;

//        System.out.println(row1);
//        System.out.println(row2);
//        System.out.println(context);

        int separatorIdx = ((SeparateRecursiveRow<I>) row1.getSuccessor(symbolIdx))
                .getContextDistinctionSeparator(row2.getSuccessor(symbolIdx), context);
        assert separatorIdx != -1;
        return Word.fromWords(Word.fromLetter(inputAlphabet.getSymbol(symbolIdx)), separators.get(separatorIdx));
    }

    public boolean areEquivalent(Row<I> prime, Row<I> row, BitSet context){
//        System.out.println(prime + " equivalent to " + row + " " + ((SeparateRecursiveRow<I>) row).contextEquivalentTo(prime, context));
        return ((SeparateRecursiveRow<I>) row).contextEquivalentTo(prime, context);
    }
}
