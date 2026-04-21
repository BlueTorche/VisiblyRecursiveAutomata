package umons.ac.be.vra;

import umons.ac.be.dfa.DeterminsticFiniteAutomaton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProceduralAutomaton extends DeterminsticFiniteAutomaton<VRAState> {
    private final String proceduralSymbol;

    public ProceduralAutomaton(String proceduralSymbol, VRAState initialState) {
        super(initialState);
        this.proceduralSymbol = proceduralSymbol;
        fillProceduralAutomaton();
    }

    public String getProceduralSymbol() {
        return proceduralSymbol;
    }

    private void fillProceduralAutomaton() {
        Set<VRAState> visited = new HashSet<>();
        VRAState s = this.getInitalState();
        List<VRAState> toVisit = new ArrayList<>();
        toVisit.add(s);
        while (!toVisit.isEmpty()) {
            s = toVisit.removeLast();
            if (visited.contains(s)) continue;
            s.setProceduralAutomaton(this);
            visited.add(s);
            for (VRAState state : s.getAllTransitions().values()) {
                if (!visited.contains(state)) {
                    toVisit.add(state);
                }
            }
        }
    }
}
