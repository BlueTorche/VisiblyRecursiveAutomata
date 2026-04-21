package umons.ac.be.vraalphabet;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.AbstractAlphabet;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractVRAlphabet<I> extends AbstractAlphabet<I> implements VRAlphabet<I> {
    private final VPAlphabet<I> vpAlphabet;
    private final Alphabet<I> proceduralAlphabet;

    public AbstractVRAlphabet(
            Alphabet<I> internalAlphabet,
            Alphabet<I> callAlphabet,
            Alphabet<I> returnAlphabet,
            Alphabet<I> proceduralAlphabet) {
        this(validateDisjointness(proceduralAlphabet, internalAlphabet, callAlphabet, returnAlphabet),
                internalAlphabet,
                callAlphabet,
                returnAlphabet,
                proceduralAlphabet);
    }

    // utility constructor to prevent finalizer attacks, see SEI CERT Rule OBJ-11
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private AbstractVRAlphabet(boolean valid,
                               Alphabet<I> internalAlphabet,
                               Alphabet<I> callAlphabet,
                               Alphabet<I> returnAlphabet,
                               Alphabet<I> proceduralAlphabet) {
        this.vpAlphabet = new DefaultVPAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);
        this.proceduralAlphabet = proceduralAlphabet;
    }

    @Override
    public VPAlphabet<I> getInputAlphabet() {
        return vpAlphabet;
    }

    @Override
    public Alphabet<I> getInternalAlphabet() {
        return vpAlphabet.getInternalAlphabet();
    }

    @Override
    public Alphabet<I> getCallAlphabet() {
        return vpAlphabet.getCallAlphabet();
    }

    @Override
    public Alphabet<I> getReturnAlphabet() {
        return vpAlphabet.getReturnAlphabet();
    }

    @Override
    public Alphabet<I> getProceduralAlphabet() {
        return proceduralAlphabet;
    }

    @Override
    public int size() {
        return vpAlphabet.size() + proceduralAlphabet.size();
    }


    @Override
    public I getSymbol(int index) {
        int localIndex = index;

        if (localIndex < vpAlphabet.size()) {
            return vpAlphabet.getSymbol(localIndex);
        } else {
            localIndex -= vpAlphabet.size();
        }

        if (localIndex < proceduralAlphabet.size()) {
            return proceduralAlphabet.getSymbol(localIndex);
        } else {
            throw new IllegalArgumentException("Index not within its expected bounds");
        }
    }

    @Override
    public int getSymbolIndex(I symbol) {
        int offset = 0;

        if (vpAlphabet.containsSymbol(symbol)) {
            return vpAlphabet.getSymbolIndex(symbol);
        } else {
            offset += vpAlphabet.size();
        }

        if (proceduralAlphabet.containsSymbol(symbol)) {
            return offset + proceduralAlphabet.getSymbolIndex(symbol);
        } else {
            throw new IllegalArgumentException("Alphabet does not contain the queried symbol");
        }
    }

    @Override
    public SymbolType getSymbolType(I symbol) {
        if (proceduralAlphabet.containsSymbol(symbol)) {
            return SymbolType.PROCEDURAL;
        }
        return switch (vpAlphabet.getSymbolType(symbol)) {
            case VPAlphabet.SymbolType.CALL -> SymbolType.CALL;
            case VPAlphabet.SymbolType.RETURN -> SymbolType.RETURN;
            case VPAlphabet.SymbolType.INTERNAL -> SymbolType.INTERNAL;
        };
    }

    @Override
    public int getNumProcedurals() {
        return proceduralAlphabet.size();
    }

    @Override
    public boolean containsSymbol(I symbol) {
        return vpAlphabet.containsSymbol(symbol) || proceduralAlphabet.containsSymbol(symbol);
    }

    @SafeVarargs
    private static <I> boolean validateDisjointness(Collection<I> source,
                                                    Collection<I>... rest) {
        final Set<I> sourceAsSet = new HashSet<>(source);
        final int initialSize = sourceAsSet.size();

        for (Collection<I> c : rest) {
            sourceAsSet.removeAll(c);
        }

        if (sourceAsSet.size() < initialSize) {
            throw new IllegalArgumentException(
                    "The set of " + SymbolType.PROCEDURAL + " symbols is not disjoint with the sets of other symbols.");
        }

        return true;
    }
}
