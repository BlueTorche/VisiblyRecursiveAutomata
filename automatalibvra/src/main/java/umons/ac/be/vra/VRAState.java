package umons.ac.be.vra;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public final class VRAState<S, I, P> {
    private final @Nullable VRAState<S, I, P> prev;
    private final @Nullable Map<P, S> procedureStates;

    public VRAState(Map<P, S> procedureStates) {
        this.prev = null;
        this.procedureStates = procedureStates;
    }

    private VRAState(VRAState<S, I, P> prev, Map<P, S> procedureStates) {
        this.prev = prev;
        this.procedureStates = procedureStates;
    }

    VRAState<S, I, P> push( Map<P, S> newProcedureStates) {
        return new VRAState<>(this, newProcedureStates);
    }

    VRAState<S, I, P> pop() {
        return prev;
    }

    S getCurrentState(P procedure) {
        assert procedureStates != null;
        return procedureStates.getOrDefault(procedure, null) ;
    }
}
