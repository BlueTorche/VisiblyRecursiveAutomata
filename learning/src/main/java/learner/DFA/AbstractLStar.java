package learner.DFA;

import umons.ac.be.learner.Learner;
import umons.ac.be.ObservationTable.RegularObservationTable;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.word.Word;
import umons.ac.be.oracle.rl.Oracle;

import java.util.HashMap;

public class AbstractLStar<I> implements Learner<I, FastDFA<I>> {
    GrowingAlphabet<I> alphabet;
    Oracle<I, DFA<?, I>> oracle;
    RegularObservationTable<I> observationTable;
    HashMap<Word<I>, Boolean> gatheredKnowledge = new HashMap<>();

    public AbstractLStar(GrowingAlphabet<I> alphabet, Oracle<I, DFA<?, I>> oracle) {
        this.observationTable = new RegularObservationTable<>(alphabet, this);
        this.alphabet = alphabet;
        this.oracle = oracle;
    }

    @Override
    public FastDFA<I> constructHypothesis() {
        return (FastDFA<I>) observationTable.constructHypothesis();
    }

    @Override
    public boolean askMembershipQuery(Word<I> word) {
        if (!gatheredKnowledge.containsKey(word)) {
            System.out.println("asking MQ for " + word);
            gatheredKnowledge.put(word, oracle.MembershipQuery(word));
        }
        return gatheredKnowledge.get(word);
    }

    @Override
    public void displayStats() {

    }

    @Override
    public FastDFA<I> learn() {
        observationTable.initialize();
        System.out.println(observationTable);
        for(int i = 0; i < 10; i++) {
            observationTable.enforce();
            System.out.println(observationTable);

            FastDFA<I> hypothesis = constructHypothesis();
            Word<I> cx = oracle.EquivalenceQuery(hypothesis);
            if (cx == null){
                return hypothesis;
            } else {
                processCounterExample(cx);
            }
            System.out.println("Processed Couterexample:" + cx);
            System.out.println(observationTable);
        }
        return constructHypothesis();
    }

    @Override
    public void processCounterExample(Word<I> cx) {
        observationTable.addRepresentative(cx);
    }
}
