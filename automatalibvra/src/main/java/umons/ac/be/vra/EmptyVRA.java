package umons.ac.be.vra;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

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
    public @Nullable Void getTransition(Void unused, I i) {
        return null;
    }

    @Override
    public boolean isAccepting(Void unused) {
        return false;
    }

    @Override
    public @Nullable Void getInitialState() {
        return null;
    }

    @Override
    public boolean accepts(Word<I> word) {
        return false;
    }
}
