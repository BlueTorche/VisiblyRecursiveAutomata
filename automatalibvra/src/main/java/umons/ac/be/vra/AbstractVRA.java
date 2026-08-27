package umons.ac.be.vra;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.graph.concept.GraphViewable;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

abstract class AbstractVRA<S, I, M extends UniversalDeterministicAutomaton<S, I, ?, ?, ?>>
        implements VRA<S, I,  M> {

    private final VRAlphabet<I> vrAlphabet;
    private final Map<I, M> procedures;
    private final M startingProcedure;

    public AbstractVRA(VRAlphabet<I> vrAlphabet,
                       Map<I, M> procedures,
                       M startingProcedure) {
        this.vrAlphabet = vrAlphabet;
        this.procedures = procedures;
        this.startingProcedure = startingProcedure;
    }

    @Override
    public VRAlphabet<I> getInputAlphabet() {
        return vrAlphabet;
    }

    @Override
    public Alphabet<I> getAutomatonAlphabet() {
        return vrAlphabet.getAutomatonAlphabet();
    }

    @Override
    public M getStartingProcedure() {
        return this.startingProcedure;
    }

    @Override
    public Map<I, M> getProcedures() {
        return procedures;
    }


    @Override
    public VRAState<S, I, M> getInitialState() {
        Map<M, Set<S>> initialLocation = new HashMap<>();
        initialLocation.put(startingProcedure, Set.of(Objects.requireNonNull(startingProcedure.getInitialState())));
        return new VRAState<>(initialLocation);
    }

    @Override
    public Map<I, M> getAllProcedures(){
        final Map<I, M> allProcedures = new HashMap<>(procedures);
        allProcedures.put((I) "S", startingProcedure);
        return allProcedures;
    }

    @Override
    public boolean accepts(Word<I> word) {
        VRAState<S, I, M> currentState = getInitialState();

        for (I symbol: word) {
            currentState = getTransition(currentState, symbol);
            if (currentState.getCurrentProcedures().isEmpty()) {
                return false;
            }
        }
        return isAccepting(currentState);
    }

    @Override
    public void removeProcedure(I proceduralSymbol){
        procedures.remove(proceduralSymbol);
    }

    @Override
    public void visualizeIndividually() {
//        System.out.println("Starting Procedure");
//        Visualization.visualize((GraphViewable) startingProcedure);
        for (Map.Entry<I, M> procedure: procedures.entrySet()) {
            System.out.println("Procedure " + procedure.getKey());
            Visualization.visualize((GraphViewable) procedure.getValue());
        }
    }
}
