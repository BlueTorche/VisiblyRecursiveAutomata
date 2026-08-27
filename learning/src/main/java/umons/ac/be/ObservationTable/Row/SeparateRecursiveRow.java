package umons.ac.be.ObservationTable.Row;

import umons.ac.be.ObservationTable.Row.Content.RecursiveContent;
import net.automatalib.word.Word;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class SeparateRecursiveRow<I> extends IsomorphicRecursiveRow<I> {
    Map<Word<I>, SeparateRecursiveRow<I>> parents = new HashMap<>();

    public SeparateRecursiveRow(Word<I> prefix, int SeparatorSize) {
        super(prefix, SeparatorSize);
    }

    public boolean contextEquivalentTo(Row<I> prime, BitSet context){
        return ((RecursiveContent) contents).contextEquivalent((RecursiveContent) prime.getContent(), context) == -1;
    }

    public boolean isContextPrime(Word<I> recPrime){
        return !parents.containsKey(recPrime);
    }

    public void setContextPrime(Word<I> recPrime) {
        parents.remove(recPrime);
    }

    public void setContextParent(SeparateRecursiveRow<I> row, Word<I> recPrime){
        parents.put(recPrime, row);
    }

    public SeparateRecursiveRow<I> getContextParent(Word<I> recPrime){
        return parents.get(recPrime);
    }

    public int contextConsistentTo(Row<I> other, BitSet context){
        for (int idx = 0; idx < successorRows.size(); idx++) {
//            System.out.println(successorRows.get(idx) + " -- " + other.getSuccessor(idx) );
//            System.out.println(((SeparateRecursiveRow<I>) successorRows.get(idx)).contextEquivalentTo(other.getSuccessor(idx), context));
            if (!((SeparateRecursiveRow<I>) successorRows.get(idx)).contextEquivalentTo(other.getSuccessor(idx), context)) {
                return idx;
            }
        }
        return -1;
    }

    public int getContextDistinctionSeparator(Row<I> other, BitSet context){
        return ((RecursiveContent) contents).contextEquivalent((RecursiveContent) other.getContent(), context);
    }

    @Override
    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString() + "\t");
        for (Map.Entry<Word<I>, SeparateRecursiveRow<I>> entry : parents.entrySet()) {
            toRet.append(entry.getKey()).append("~").append(entry.getValue().getPrefix()).append("\t;\t");
        }
        return toRet.toString();
    }

    public SeparateRecursiveRow<I> getSuccessor(int idx, Word<I> recEquivClass) {
        SeparateRecursiveRow<I> directSuccessor = (SeparateRecursiveRow<I>) getSuccessor(idx);
        return directSuccessor.isContextPrime(recEquivClass) ?
                directSuccessor : directSuccessor.getContextParent(recEquivClass);
    }
}
