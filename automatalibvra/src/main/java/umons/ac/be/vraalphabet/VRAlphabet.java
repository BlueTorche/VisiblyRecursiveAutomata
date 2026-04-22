package umons.ac.be.vraalphabet;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.Alphabet;

public interface VRAlphabet<I> extends VPAlphabet<I> {
    Alphabet<I> getInputAlphabet();

    Alphabet<I> getAutomatonAlphabet();

    /**
     * Returns the procedural symbols of {@code this} alphabet as a (sub-) alphabet.
     *
     * @return the procedural alphabet
     */
    Alphabet<I> getProceduralAlphabet();

    /**
     * Returns all procedural symbols of {@code this} alphabet linked to the given call symbol as a (sub-) alphabet.
     *
     * @param callSymbol is the call symbol whose the procedural symbols returned are linked to.
     *
     * @return the procedural alphabet linked to the given call symbol
     */
    Alphabet<I> getProceduralAlphabetFromCall(I callSymbol);

    /**
     * Returns all procedural symbols of {@code this} alphabet linked to the given return symbol as a (sub-) alphabet.
     *
     * @param retSymbol is the return symbol whose the procedural symbols returned are linked to.
     *
     * @return the procedural alphabet linked to the given call symbol
     */
    Alphabet<I> getProceduralAlphabetFromReturn(I retSymbol);

    /**
     * Returns the call symbol of {@code this} alphabet linked to the given procedural symbol.
     *
     * @param proceduralSymbol is the procedural symbol whose the call symbol returned are linked to.
     *
     * @return the procedural alphabet linked to the given call symbol
     */
    I getCallSymbolFromProceduralSymbol(I proceduralSymbol);

    /**
     * Returns the return symbol of {@code this} alphabet linked to the given procedural symbol.
     *
     * @param proceduralSymbol is the procedural symbol whose the call symbol returned are linked to.
     *
     * @return the procedural alphabet linked to the given call symbol
     */
    I getReturnSymbolFromProceduralSymbol(I proceduralSymbol);

    /**
     * Returns all procedural symbols of {@code this} alphabet linked to the given call and return symbols
     * as a (sub-) alphabet.
     *
     * @param callSymbol is the return symbol whose the procedural symbols returned are linked to.
     * @param retSymbol is the return symbol whose the procedural symbols returned are linked to.
     *
     * @return the procedural alphabet linked to the given call symbol
     */
    Alphabet<I> getProceduralAlphabetFromCallAndReturn(I callSymbol, I retSymbol);


    /**
     * Add a procedural symbol to the procedural alphabet of {@code this} alphabet,
     * and link the symbol to a call and a return symbol.
     *
     * @param proceduralSymbol is the procedural symbol to add.
     * @param callSymbol is the call symbol linked to the procedural symbol.
     * @param returnSymbol is the return symbol linked to the procedural symbol.
     */
    void addProceduralSymbol(I proceduralSymbol, I callSymbol, I returnSymbol);


    /**
     * The {@link Alphabet#size()} variant for the procedural alphabet.
     *
     * @return the number of call symbols
     */
    int getNumProcedurals();


    /**
     * Returns whether the given symbol is a return symbol of {@code this} alphabet.
     *
     * @param symbol
     *         the symbol to analyze
     *
     * @return {@code true} if the given symbol is a return symbol of this alphabet, {@code false} otherwise
     *
     * @throws IllegalArgumentException
     *         if the provided symbol does not belong to the alphabet.
     */
    default boolean isProceduralSymbol(I symbol) {
        return getProceduralAlphabet().contains(symbol);
    }
}
