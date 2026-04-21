package umons.ac.be.vra;

import net.automatalib.automaton.UniversalDeterministicAutomaton;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.Map;

public abstract class AbstractVRA<S, I, M extends UniversalDeterministicAutomaton<?, I, ?, ?, ?>> implements VRA<S, I, M> {
    @Override
    public VRAlphabet<I> getInputAlphabet() {
        return null;
    }

    @Override
    public VRAlphabet<I> getVRAlphabet() {
        return null;
    }

    @Override
    public I getStartingProcedure() {
        return null;
    }

    @Override
    public Map<I, M> getProcedures() {
        return Map.of();
    }

    @Override
    public @Nullable S getTransition(S s, I i) {
        return null;
    }

    @Override
    public boolean isAccepting(S s) {
        return false;
    }

    @Override
    public @Nullable S getInitialState() {
        return null;
    }
}
