package umons.ac.be.vra;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public abstract class AbstractVRA<S, I, M extends UniversalDeterministicAutomaton<S, I, ?, ?, ?>>
        implements VRA<VRAState<S, I, M>, I,  M> {

    private final VRAlphabet<I> vrAlphabet;
    private final Map<I, M> procedures;
    private final M startingProcedure;

    public AbstractVRA(VRAlphabet<I> vrAlphabet, Map<I, M> procedures, M startingProcedure) {
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
}
