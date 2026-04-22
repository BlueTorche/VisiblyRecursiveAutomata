package umons.ac.be.vraalphabet;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.*;
import net.automatalib.common.util.Pair;

import java.util.*;

public abstract class AbstractVRAlphabet<I> extends AbstractVPAlphabet<I> implements VRAlphabet<I> {
    private final GrowingAlphabet<I> proceduralAlphabet;
    private final HashMap<I, Pair<I, I>> proceduralToCallReturn;
    private final HashMap<I, Alphabet<I>> callToProcedural;
    private final HashMap<I, Alphabet<I>> returnToProcedural;

    public AbstractVRAlphabet(
            Alphabet<I> internalAlphabet,
            Alphabet<I> callAlphabet,
            Alphabet<I> returnAlphabet) {
        super(internalAlphabet, callAlphabet, returnAlphabet);
        this.proceduralAlphabet = new GrowingMapAlphabet<>();
        this.proceduralToCallReturn = new HashMap<>();
        this.callToProcedural = new HashMap<>();
        for(I c: callAlphabet) { callToProcedural.put(c, new GrowingMapAlphabet<>()); }
        this.returnToProcedural = new HashMap<>();
        for(I r: returnAlphabet) { returnToProcedural.put(r, new GrowingMapAlphabet<>()); }
    }

    @Override
    public void addProceduralSymbol(I proceduralSymbol, I callSymbol, I returnSymbol) {
        assertDisjointness(
                proceduralSymbol,
                getInternalAlphabet(),
                getCallAlphabet(),
                getReturnAlphabet(),
                proceduralAlphabet);
        if (!getCallAlphabet().contains(callSymbol)) {
            throw new IllegalArgumentException("The symbol " + callSymbol + " is not in the call alphabet.");
        }
        if (!getReturnAlphabet().contains(returnSymbol)) {
            throw new IllegalArgumentException("The symbol " + returnSymbol + " is not in the return alphabet.");
        }

        proceduralAlphabet.addSymbol(proceduralSymbol);
        proceduralToCallReturn.put(proceduralSymbol, Pair.of(callSymbol, returnSymbol));
        callToProcedural.get(callSymbol).add(proceduralSymbol);
        returnToProcedural.get(returnSymbol).add(proceduralSymbol);
    }

    @Override
    public Alphabet<I> getProceduralAlphabetFromCall(I callSymbol) {
        if (!getCallAlphabet().contains(callSymbol)) {
            throw new IllegalArgumentException("The symbol " + callSymbol + " is not in the call alphabet.");
        }
        return callToProcedural.get(callSymbol);
    }

    @Override
    public Alphabet<I> getProceduralAlphabetFromReturn(I retSymbol) {
        if (!getReturnAlphabet().contains(retSymbol)) {
            throw new IllegalArgumentException("The symbol " + retSymbol + " is not in the call alphabet.");
        }
        return returnToProcedural.get(retSymbol);
    }

    @Override
    public I getCallSymbolFromProceduralSymbol(I proceduralSymbol) {
        if (!proceduralAlphabet.contains(proceduralSymbol)) {
            throw new IllegalArgumentException("The symbol " + proceduralSymbol + " is not in the procedural alphabet.");
        }
        return proceduralToCallReturn.get(proceduralSymbol).getFirst();
    }

    @Override
    public I getReturnSymbolFromProceduralSymbol(I proceduralSymbol) {
        if (!proceduralAlphabet.contains(proceduralSymbol)) {
            throw new IllegalArgumentException("The symbol " + proceduralSymbol + " is not in the procedural alphabet.");
        }
        return proceduralToCallReturn.get(proceduralSymbol).getSecond();
    }

    @Override
    public Alphabet<I> getProceduralAlphabetFromCallAndReturn(I callSymbol, I retSymbol) {
        if (!getCallAlphabet().contains(callSymbol)) {
            throw new IllegalArgumentException("The symbol " + callSymbol + " is not in the call alphabet.");
        }
        if (!getReturnAlphabet().contains(retSymbol)) {
            throw new IllegalArgumentException("The symbol " + retSymbol + " is not in the call alphabet.");
        }
        Alphabet<I> intersection = new GrowingMapAlphabet<>();
        for(I proceduralSymbol : callToProcedural.get(callSymbol)) {
            if (returnToProcedural.get(retSymbol).contains(proceduralSymbol)) {
                intersection.add(proceduralSymbol);
            }
        }
        return intersection;
    }


    @Override
    public Alphabet<I> getInputAlphabet() {
        return Alphabets.fromCollections(
                getInternalAlphabet(),
                getCallAlphabet(),
                getReturnAlphabet());
    }

    @Override
    public Alphabet<I> getAutomatonAlphabet() {
        return Alphabets.fromCollections(getInternalAlphabet(), proceduralAlphabet);
    }

    @Override
    public Alphabet<I> getProceduralAlphabet() {
        return proceduralAlphabet;
    }

    @Override
    public int size() {
        return super.size() + proceduralAlphabet.size();
    }


    @Override
    public I getSymbol(int index) {
        int localIndex = index;

        if (localIndex < super.size()) {
            return super.getSymbol(localIndex);
        } else {
            localIndex -= super.size();
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

        if (super.containsSymbol(symbol)) {
            return super.getSymbolIndex(symbol);
        } else {
            offset += super.size();
        }

        if (proceduralAlphabet.containsSymbol(symbol)) {
            return offset + proceduralAlphabet.getSymbolIndex(symbol);
        } else {
            throw new IllegalArgumentException("Alphabet does not contain the queried symbol");
        }
    }

    @Override
    public int getNumProcedurals() {
        return proceduralAlphabet.size();
    }

    @Override
    public boolean containsSymbol(I symbol) {
        return proceduralAlphabet.containsSymbol(symbol) || super.containsSymbol(symbol);
    }

    @Override
    public Comparator<I> thenComparing(Comparator<? super I> other) {
        return super.thenComparing(other);
    }

    @SafeVarargs
    private static <I> void assertDisjointness(I source, Alphabet<I>... rest) {
        for (Alphabet<I> alphabet: rest) {
            if (alphabet.contains(source)) {
                throw new IllegalArgumentException(
                        "The symbol of " + source + " is already in the sets of other symbols.");
            }
        }
    }
}
