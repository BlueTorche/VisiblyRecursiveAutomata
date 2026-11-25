package umons.ac.be.vra;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EmptyVRA<I> implements VRA<Void, I,  DFA<Void, I>> {
    private final VRAlphabet<I> alphabet;

    public EmptyVRA(VRAlphabet<I> alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public VRAlphabet<I> getInputAlphabet() {
        return null;
    }

    @Override
    public Alphabet<I> getAutomatonAlphabet() {
        return null;
    }

    @Override
    public DFA<Void, I> getStartingProcedure() {
        return null;
    }

    @Override
    public Map<I, DFA<Void, I>> getProcedures() {
        return null;
    }

    @Override
    public Map<I, DFA<Void, I>> getAllProcedures() {
        return Map.of();
    }

    @Override
    public boolean accepts(Word<I> word) {
        return false;
    }

    @Override
    public void removeProcedure(I proceduralSymbol) { }

    @Override
    public VRA<Void, I, DFA<Void, I>> removeBinStatesAndAutomata() {
        return null;
    }

    @Override
    public void visualizeIndividually() { }

    @Override
    public int numberOfTrans() {
        return 0;
    }

    @Override
    public Collection<VRAState<Void, I, DFA<Void, I>>> getStates() {
        return List.of();
    }

    @Override
    public Boolean getStateProperty(VRAState<Void, I, DFA<Void, I>> voidIDFAVRAState) {
        return null;
    }

    @Override
    public Void getTransitionProperty(VRAState<Void, I, DFA<Void, I>> voidIDFAVRAState) {
        return null;
    }

    @Override
    public DeterministicAcceptorTS<VRAState<Void, I, DFA<Void, I>>, I> getSemantics() {
        return null;
    }

    @Override
    public VRAState<Void, I, DFA<Void, I>> getSuccessor(VRAState<Void, I, DFA<Void, I>> voidIDFAVRAState) {
        return null;
    }
}
