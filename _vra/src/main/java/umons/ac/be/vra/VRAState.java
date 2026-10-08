package umons.ac.be.vra;

import umons.ac.be.dfa.State;

import java.util.Set;
import java.util.stream.Collectors;

public class VRAState extends State<VRAState> {
    private ProceduralAutomaton proceduralAutomaton;

    public VRAState(String name, boolean isFinal) {
        super(name, isFinal);
    }

    public VRAState(String name, boolean isFinal, ProceduralAutomaton proceduralAutomaton) {
        super(name, isFinal);
        this.proceduralAutomaton = proceduralAutomaton;
    }

    public ProceduralAutomaton getProceduralAutomaton() {
        return proceduralAutomaton;
    }

    public void setProceduralAutomaton(ProceduralAutomaton proceduralAutomaton) {
        this.proceduralAutomaton = proceduralAutomaton;
    }

    public Set<String> getProceduralTransitions(VRAAlphabet alphabet) {
        return this.getTransitionsSymbols().stream()
                .filter(symbol -> alphabet.kindOfSymbol(symbol) == VRAAlphabet.SymbolType.PROCEDURAL)
                .collect(Collectors.toSet());
    }
}
