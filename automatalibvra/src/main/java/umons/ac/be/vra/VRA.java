package umons.ac.be.vra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.automatalib.alphabet.ProceduralInputAlphabet;
import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.automaton.concept.FiniteRepresentation;
import net.automatalib.automaton.concept.InputAlphabetHolder;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.procedural.ProceduralGraphView;
import net.automatalib.automaton.simple.SimpleAutomaton;
import net.automatalib.graph.Graph;
import net.automatalib.graph.concept.GraphViewable;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.ts.simple.SimpleTS;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

public interface VRA<S, I, M extends UniversalDeterministicAutomaton<?, I, ?, ?, ?>>
        extends FiniteRepresentation, GraphViewable, InputAlphabetHolder<I>, DeterministicAcceptorTS<S, I> {
    VRAlphabet<I> getInputAlphabet();

    /**
     * Refinement of {@link InputAlphabetHolder#getInputAlphabet()} to add the constraint that {@code this} system
     * operates on {@link ProceduralInputAlphabet}s.
     *
     * @return the input alphabet
     */
    VRAlphabet<I> getVRAlphabet();

    /**
     * Convenience method for {@link #getProceduralInputs(Collection)} which uses the
     * {@link #getInputAlphabet() input alphabet} of {@code this} system as {@code constraints}.
     *
     * @return a collection of defined inputs for {@code this} system's procedures.
     */
    default Collection<I> getProceduralInputs(Collection<I> constraints) {
        final VRAlphabet<I> alphabet = getVRAlphabet();
        final Map<I, M> procedures = getProcedures();

        final List<I> result = new ArrayList<>(Math.min(alphabet.size() - 1, constraints.size()));

        for (I i : constraints) {
            if (procedures.containsKey(i) || alphabet.isInternalSymbol(i)) {
                result.add(i);
            }
        }

        return result;
    }

    /**
     * Returns the initial (represented via its {@link VRAlphabet#getProceduralAlphabet()} procedural symbol})
     * of this VRA.
     *
     * @return the initial procedure, may be {@code null} if undefined
     */
    I getStartingProcedure();

    /**
     * Returns a {@link Map} from {@link VRAlphabet#getProceduralAlphabet() call symbols} to the procedures of
     * {@code this} system. Note that a (non-minimal) {@link VRA} may not contain a procedure for every
     * procedural symbol.
     *
     * @return the procedures of this system
     */
    Map<I, M> getProcedures();

    /**
     * Convenience method for {@link #getProcedures()} to quickly return the procedure of a given call symbol.
     *
     * @param proceduralSymbol
     *         the procedural symbol
     *
     * @return the corresponding procedure. May be {@code null} if {@code this} system does not have a procedure for the
     * given call symbol.
     *
     * @see #getProcedures()
     */
    default @Nullable M getProcedure(I proceduralSymbol) {
        assert getInputAlphabet().isProceduralSymbol(proceduralSymbol);
        return getProcedures().get(proceduralSymbol);
    }

    /**
     * Returns the size of {@code this} system which is given by the sum of the sizes of all
     * {@link #getProcedures() procedures}. Note that this value does not necessarily correspond to the classical notion
     * of {@link SimpleAutomaton#size()}, since semantically a {@link VRA} may be infinite-sized {@link SimpleTS}.
     *
     * @return the size of {@code this} system
     */
    @Override
    default int size() {
        int size = 0;

        for (M p : getProcedures().values()) {
            size += p.size();
        }

        return size;
    }

    @Override
    default Graph<?, ?> graphView() {
        final VRAlphabet<I> alphabet = this.getInputAlphabet();
        return new ProceduralGraphView<>(alphabet.getInternalAlphabet(),
                this.getProceduralInputs(alphabet),
                this.getProcedures());
    }
}