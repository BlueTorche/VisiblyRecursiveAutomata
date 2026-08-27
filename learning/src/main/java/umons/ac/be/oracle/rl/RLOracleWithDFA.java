package umons.ac.be.oracle.rl;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

public class RLOracleWithDFA<I> implements Oracle<I, DFA<?, I>> {
    DFA<?, I> dfa;
    int membershipCounter;
    int equivalenceCounter;

    public RLOracleWithDFA(DFA<?, I> automaton) {
        this.dfa = automaton;
        this.membershipCounter = 0;
        this.equivalenceCounter = 0;
    }

    @Override
    public boolean MembershipQuery(Word<I> word) {
        this.membershipCounter++;
        return this.dfa.accepts(word);
    }

    @Override
    public Word<I> EquivalenceQuery(DFA<?, I> hypothesis) {
        this.equivalenceCounter++;

        for(int length = 0;  length < 15; length++) {
            // TODO generate all word and test them
        }

        return null;
    }

    @Override
    public void displayStats() {
        System.out.println("#MQ: " + this.membershipCounter);
        System.out.println("#EQ: " + this.equivalenceCounter);
    }

    @Override
    public Alphabet<I> getInputAlphabet() {
        return null;
    }
}
