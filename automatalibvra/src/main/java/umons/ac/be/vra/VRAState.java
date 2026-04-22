package umons.ac.be.vra;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;

public final class VRAState<S, I, P> {
    private final @Nullable VRAState<S, I, P> prev;
    private final Map<P, Set<S>> procedureStates;

    public VRAState(Map<P, Set<S>> procedureStates) {
        this.prev = null;
        this.procedureStates = procedureStates;
    }

    private VRAState(VRAState<S, I, P> prev, Map<P, Set<S>> procedureStates) {
        this.prev = prev;
        this.procedureStates = procedureStates;
    }

    VRAState<S, I, P> push( Map<P, Set<S>> newProcedureStates) {
        return new VRAState<>(this, newProcedureStates);
    }

    VRAState<S, I, P> pop() {
        return prev;
    }

    VRAState<S, I, P> updateProcedureStates(Map<P, Set<S>> nextProcedureStates){
        return new VRAState<>(prev, nextProcedureStates);
    }

    Set<P> getCurrentProcedures() {
        assert procedureStates != null;
        return procedureStates.keySet();
    }

    Set<S> getCurrentStatesFromProcedure(P procedure) {
        assert procedureStates != null;
        return procedureStates.getOrDefault(procedure, null) ;
    }

    boolean isAccepting() {
        if (prev != null) {
            return false;
        }
        for(P procedure: procedureStates.keySet()) {
            for(S state: procedureStates.get(procedure)) {
                if (procedure.isAccepting(state)) {
                    return true;
                }
            }
        }
        return false;
    }
}
