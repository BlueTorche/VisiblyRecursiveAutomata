package umons.ac.be.JSONutils;

import be.ac.umons.jsonschematools.AbstractConstants;
import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility functions for oracles.
 *
 * @author Gaëtan Staquet
 */
public class Utils {
    private Utils() {
    }

    /**
     * Converts an Automatalib's Word into a Java String.
     *
     * @param word The word
     * @return The string constructed from the word.
     */
    public static String wordToString(final Word<Character> word) {
        StringBuilder builder = new StringBuilder(word.size());
        for (Character character : word) {
            builder.append(character);
        }
        return builder.toString();
    }

    /**
     * Counts the number of unmatched { and [ in a String.
     *
     * @param word The string
     * @return The number of unmatched { and [
     */
    public static int countUnmatched(final String word) {
        int numberUnmatchedOpen = 0;
        boolean inString = false;
        boolean previousWasEscape = false;
        for (int i = 0; i < word.length(); i++) {
            char character = word.charAt(i);
            if (!previousWasEscape && (character == '"' || character == '\'')) {
                inString = !inString;
            }

            if (!inString && (character == '{' || character == '[')) {
                numberUnmatchedOpen++;
            } else if (!inString && (character == '}' || character == ']')) {
                numberUnmatchedOpen--;
            }
            previousWasEscape = (character == '\\');
        }

        return numberUnmatchedOpen;
    }

    /**
     * Tests whether the provided string encodes a valid JSON document.
     *
     * That is, the string must begin by {@code {} and end by {@code \}}.
     *
     * @param word
     * @return
     */
    public static boolean validWord(final String word) {
        if (word.isEmpty() || word.charAt(0) != '{' || word.charAt(word.length() - 1) != '}') {
            return false;
        }

        int numberUnmatchedOpen = 0;
        boolean firstObject = true;
        boolean inString = false;
        boolean previousWasEscape = false;
        boolean previousWasComma = false;
        for (int i = 0; i < word.length(); i++) {
            char character = word.charAt(i);
            if (numberUnmatchedOpen == 0 && !firstObject) {
                return false;
            }
            if (!previousWasEscape && (character == '"' || character == '\'')) {
                inString = !inString;
            }
            if (!inString) {
                if (character == '{' || character == '[') {
                    if (numberUnmatchedOpen == 0) {
                        firstObject = false;
                    }
                    numberUnmatchedOpen++;
                    previousWasComma = false;
                } else if (character == '}' || character == ']') {
                    if (numberUnmatchedOpen == 0) {
                        return false;
                    }
                    if (previousWasComma) {
                        return false;
                    }
                    numberUnmatchedOpen--;
                    previousWasComma = false;
                } else if (character == ',') {
                    if (previousWasComma) {
                        return false;
                    }
                    previousWasComma = true;
                } else {
                    previousWasComma = false;
                }
            }

            previousWasEscape = (character == '\\');
        }

        return true;
    }

    public static boolean validWordObject(final Word<JSONSymbol> word) {
        if (word.size() < 2) {
            return false;
        }
        if (!word.getSymbol(0).equals(JSONSymbol.openingCurlyBraceSymbol) &&
            !word.getSymbol(word.size()-1).equals(JSONSymbol.closingCurlyBraceSymbol)) {
            return false;
        }
        boolean shouldBeKey = true;
        boolean shouldBeValue = false;
        boolean shouldBeComma = false;
        for (int i = 1; i < word.size()-1; i++) {
            if (shouldBeKey && !JSONSymbol.isKeySymbol(word.getSymbol(i))) {
//                System.out.println("Invalid key symbol at index " + i + " : " + word);
                return false;
            }
            if (shouldBeComma && !word.getSymbol(i).equals(JSONSymbol.commaSymbol)) {
//                System.out.println("Invalid comma symbol at index " + i + " : " + word);
                return false;
            }
            if (shouldBeValue) {
                if (!JSONSymbol.primitiveValuesAlphabet.containsSymbol(word.getSymbol(i)) &&
                        !word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol) &&
                        !word.getSymbol(i).equals(JSONSymbol.openingBracketSymbol)) {
//                    System.out.println("Invalid value symbol at index " + i + " : " + word);
                    return false;
                }
                if (word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol) ||
                        word.getSymbol(i).equals(JSONSymbol.openingBracketSymbol)) {
                    boolean isObject = word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol);
                    WordBuilder<JSONSymbol> jsonObject = new WordBuilder<>();
                    jsonObject.add(word.getSymbol(i));
                    int depth = 0;
                    for (i = i + 1; i < word.size() && depth >= 0; ++i) {
                        if (word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol) ||
                                word.getSymbol(i).equals(JSONSymbol.openingBracketSymbol)) {
                            depth++;
                        }
                        if (word.getSymbol(i).equals(JSONSymbol.closingCurlyBraceSymbol) ||
                                word.getSymbol(i).equals(JSONSymbol.closingBracketSymbol)) {
                            depth--;
                        }
                        jsonObject.add(word.getSymbol(i));
                    }
                    i--;
                    if (isObject && !validWordObject(jsonObject.toWord())) {
//                        System.out.println("Invalid object at index " + i);
                        return false;
                    }
                    if (!isObject && !validWordArray(jsonObject.toWord())) {
//                        System.out.println("Invalid array at index " + i);
                        return false;
                    }
                }
            }

//            System.out.println("Idx " + i + " key " + shouldBeKey + " val " + shouldBeValue + " com " + shouldBeComma);
            if (shouldBeKey) {
                shouldBeKey = false;
                shouldBeValue = true;
            }
            else if (shouldBeValue) {
                shouldBeValue = false;
                shouldBeComma = true;
            } else if (shouldBeComma) {
                shouldBeComma = false;
                shouldBeKey = true;
            }
        }

        return shouldBeComma || word.size() == 2;
    }

    public static boolean validWordArray(final Word<JSONSymbol> word) {
        if (word.size() < 2) {
            return false;
        }
        if (!word.getSymbol(0).equals(JSONSymbol.openingBracketSymbol) &&
                !word.getSymbol(word.size()-1).equals(JSONSymbol.closingBracketSymbol)) {
            return false;
        }
        boolean shouldBeValue = true;
        boolean shouldBeComma = false;

        for (int i = 1; i < word.size()-1; i++) {
            if (shouldBeComma && !word.getSymbol(i).equals(JSONSymbol.commaSymbol)) {
//                System.out.println("Invalid comma symbol at index " + i + " : " + word);
                return false;
            }
            if (shouldBeValue) {
                if (!JSONSymbol.primitiveValuesAlphabet.containsSymbol(word.getSymbol(i)) &&
                        !word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol) &&
                        !word.getSymbol(i).equals(JSONSymbol.openingBracketSymbol)) {
//                    System.out.println("Invalid value symbol at index " + i + " : " + word);
                    return false;
                }
                if (word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol) ||
                        word.getSymbol(i).equals(JSONSymbol.openingBracketSymbol)) {
                    boolean isObject = word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol);
                    WordBuilder<JSONSymbol> jsonObject = new WordBuilder<>();
                    jsonObject.add(word.getSymbol(i));
                    int depth = 0;
                    for (i = i+1; i < word.size()  && depth >= 0; ++i) {
                        if (word.getSymbol(i).equals(JSONSymbol.openingCurlyBraceSymbol) ||
                                word.getSymbol(i).equals(JSONSymbol.openingBracketSymbol)) {
                            depth++;
                        }
                        if (word.getSymbol(i).equals(JSONSymbol.closingCurlyBraceSymbol) ||
                                word.getSymbol(i).equals(JSONSymbol.closingBracketSymbol)) {
                            depth--;
                        }
                        jsonObject.add(word.getSymbol(i));
                    }
                    i--;
                    if (isObject && !validWordObject(jsonObject.toWord())) {
//                        System.out.println("Invalid object at index " + i);
                        return false;
                    }
                    if (!isObject && !validWordArray(jsonObject.toWord())) {
//                        System.out.println("Invalid array at index " + i);
                        return false;
                    }
                }
            }

            shouldBeValue = !shouldBeValue;
            shouldBeComma = !shouldBeComma;
        }

        return shouldBeComma || word.size() == 2;
    }


    /**
     * We escape the "\S", "\E", "\I", and "\D" symbols in the document (to avoid
     * errors from JSONObject).
     * <p>
     * That means we replace every \\([SIDE]) by \\\\$1.
     *
     * @param string The string to escape
     * @return The escaped string
     */
    public static String escapeSymbolsForJSON(String string) {
        // We need to escape each \ in the Java code.
        return string.replaceAll("\\\\([SIDE])", "\\\\\\\\$1");
    }

    public static VPAlphabet<JSONSymbol> extractSymbolsFromSchema(final JSONSchema schema)
            throws JSONSchemaException {
        final Set<JSONSymbol> internalSymbols = new LinkedHashSet<>();
        final Set<JSONSymbol> callSymbols = new LinkedHashSet<>();
        final Set<JSONSymbol> returnSymbols = new LinkedHashSet<>();

        callSymbols.add(JSONSymbol.openingBracketSymbol);
        callSymbols.add(JSONSymbol.openingCurlyBraceSymbol);

        returnSymbols.add(JSONSymbol.closingBracketSymbol);
        returnSymbols.add(JSONSymbol.closingCurlyBraceSymbol);

        internalSymbols.add(JSONSymbol.commaSymbol);
        internalSymbols.add(JSONSymbol.trueSymbol);
        internalSymbols.add(JSONSymbol.falseSymbol);
        internalSymbols.add(JSONSymbol.nullSymbol);
        internalSymbols.add(JSONSymbol.stringSymbol);
        internalSymbols.add(JSONSymbol.integerSymbol);
        internalSymbols.add(JSONSymbol.numberSymbol);
        internalSymbols.add(JSONSymbol.enumSymbol);

        internalSymbols.add(JSONSymbol.toSymbol("\"" + AbstractConstants.stringConstant + "\":"));
        // @formatter:off
        schema.getAllKeysDefinedInSchema().stream()
                .map(k -> "\"" + k + "\":")
                .map(k -> JSONSymbol.toSymbol(k))
                .forEach(k -> internalSymbols.add(k));
        // @formatter:on

        return new DefaultVPAlphabet<>(internalSymbols, callSymbols, returnSymbols);
    }
}
