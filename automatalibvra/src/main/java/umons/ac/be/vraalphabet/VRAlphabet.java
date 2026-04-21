package umons.ac.be.vraalphabet;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.Alphabet;

public interface VRAlphabet<I> extends Alphabet<I> {
    VPAlphabet<I> getInputAlphabet();

    /**
     * Returns the procedural symbols of {@code this} alphabet as a (sub-) alphabet.
     *
     * @return the procedural alphabet
     */
    Alphabet<I> getProceduralAlphabet();

    /**
     * Returns the procedural symbols of {@code this} alphabet as a (sub-) alphabet.
     *
     * @return the procedural alphabet
     */
    Alphabet<I> getInternalAlphabet();

    /**
     * Returns the procedural symbols of {@code this} alphabet as a (sub-) alphabet.
     *
     * @return the procedural alphabet
     */
    Alphabet<I> getCallAlphabet();

    /**
     * Returns the procedural symbols of {@code this} alphabet as a (sub-) alphabet.
     *
     * @return the procedural alphabet
     */
    Alphabet<I> getReturnAlphabet();

    /**
     * The {@link Alphabet#size()} variant for the procedural alphabet.
     *
     * @return the number of call symbols
     */
    int getNumProcedurals();


    /**
     * Returns the {@link SymbolType symbol type} of the given alphabet symbol.
     *
     * @param symbol
     *         the symbol whose type should be returned
     *
     * @return the {@link SymbolType symbol type} of the given alphabet symbol.
     *
     * @throws IllegalArgumentException
     *         if the provided symbol does not belong to the alphabet.
     */
    SymbolType getSymbolType(I symbol);


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
        return getSymbolType(symbol) == SymbolType.PROCEDURAL;
    }

    /**
     * Returns whether the given symbol is a call symbol of {@code this} alphabet.
     *
     * @param symbol
     *         the symbol to analyze
     *
     * @return {@code true} if the given symbol is a call symbol of this alphabet, {@code false} otherwise
     *
     * @throws IllegalArgumentException
     *         if the provided symbol does not belong to the alphabet.
     */
    default boolean isCallSymbol(I symbol) {
        return getInputAlphabet().isCallSymbol(symbol);
    }

    /**
     * Returns whether the given symbol is a call symbol of {@code this} alphabet.
     *
     * @param symbol
     *         the symbol to analyze
     *
     * @return {@code true} if the given symbol is a call symbol of this alphabet, {@code false} otherwise
     *
     * @throws IllegalArgumentException
     *         if the provided symbol does not belong to the alphabet.
     */
    default boolean isInternalSymbol(I symbol) {
        return getInputAlphabet().isInternalSymbol(symbol);
    }

    /**
     * Returns whether the given symbol is a call symbol of {@code this} alphabet.
     *
     * @param symbol
     *         the symbol to analyze
     *
     * @return {@code true} if the given symbol is a call symbol of this alphabet, {@code false} otherwise
     *
     * @throws IllegalArgumentException
     *         if the provided symbol does not belong to the alphabet.
     */
    default boolean isReturnSymbol(I symbol) {
        return getInputAlphabet().isReturnSymbol(symbol);
    }


    /**
     * Classifies a symbol as a procedural symbol.
     */
    enum SymbolType {
        PROCEDURAL,
        INTERNAL,
        CALL,
        RETURN
    }
}
