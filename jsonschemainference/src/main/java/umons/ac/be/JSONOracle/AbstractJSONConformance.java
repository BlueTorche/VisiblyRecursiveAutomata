/*
 * ValidatingJSONDocumentsWithLearnedVPA - Learning a visibly pushdown automaton
 * from a JSON schema, and using it to validate JSON documents.
 *
 * Copyright 2022 University of Mons, University of Antwerp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package umons.ac.be.JSONOracle;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import be.ac.umons.jsonschematools.validator.DefaultValidator;
import be.ac.umons.jsonschematools.validator.Validator;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import umons.ac.be.JSONutils.JSONSymbol;
import de.learnlib.query.DefaultQuery;
import umons.ac.be.JSONutils.Utils;
import umons.ac.be.JSONutils.WordConversion;

import java.util.*;

/**
 * Base class for equivalence checks based on conformance testing of JSON
 * documents.
 * 
 * <p>
 * In our case, conformance testing means that we generate JSON documents and
 * check that the hypothesis and the classical validator agree on whether the
 * document is valid.
 * </p>
 * 
 * @author Gaëtan Staquet
 */
public abstract class AbstractJSONConformance<A extends DeterministicAcceptorTS<?, JSONSymbol>> {
    protected static final int MAX_NUMBER_SYMBOLS_GIBBERISH = 15;

    private final JSONSchema schema;
    private final Validator validator;
    private final int numberTests;
    private final boolean shuffleKeys;
    private final Random rand;
    protected final VPAlphabet<JSONSymbol> alphabet;
    private final boolean canGenerateInvalid;
    private final int maxProperties;
    private final int maxItems;
    protected int numberValid;
    protected long timeLastEQ;

    protected AbstractJSONConformance(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                      JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet) {
        this.numberTests = numberTests;
        this.schema = schema;
        this.validator = new DefaultValidator();
        this.shuffleKeys = shuffleKeys;
        this.rand = random;
        this.alphabet = alphabet;
        this.canGenerateInvalid = canGenerateInvalid;
        this.maxProperties = maxProperties;
        this.maxItems = maxItems;
    }


    protected Alphabet<JSONSymbol> getAlphabet() {
        return alphabet;
    }

    protected boolean shouldShuffleKeys() {
        return shuffleKeys;
    }

    protected int numberTests() {
        return numberTests;
    }

    protected Random getRandom() {
        return rand;
    }

    public JSONSchema getSchema() {
        return schema;
    }

    public boolean canGenerateInvalid() {
        return canGenerateInvalid;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public int getMaxProperties() {
        return maxProperties;
    }

    protected Word<JSONSymbol> generateGibberish() {
        final int nSymbols = rand.nextInt(MAX_NUMBER_SYMBOLS_GIBBERISH) + 1;
        final int sizeAlphabet = alphabet.size();
        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();
        List<JSONSymbol> calls = new ArrayList<>();
//        calls.add(JSONSymbol.toSymbol('{'));
//        wordBuilder.add(JSONSymbol.toSymbol('{'));
        for (int i = 1; i < nSymbols - calls.size(); i++) {
            JSONSymbol symbol = alphabet.getSymbol(rand.nextInt(sizeAlphabet));
            if (alphabet.isCallSymbol(symbol)) {
                calls.add(symbol);
                wordBuilder.add(symbol);
            }
            else if (alphabet.isReturnSymbol(symbol)) {
                if (calls.isEmpty()) {
                    return wordBuilder.toWord();
                }
                wordBuilder.add(calls.removeLast().equals(JSONSymbol.toSymbol('{')) ?
                        JSONSymbol.toSymbol('}') : JSONSymbol.toSymbol(']'));
            } else {
                wordBuilder.add(symbol);
            }
        }
        while (!calls.isEmpty()) {
            wordBuilder.add(calls.removeLast().equals(JSONSymbol.toSymbol('{')) ?
                    JSONSymbol.toSymbol('}') : JSONSymbol.toSymbol(']'));
        }

        return wordBuilder.toWord();
    }

    protected Word<JSONSymbol> generateGibberishByAddingSymbol(JSONObject document) {
        Word<JSONSymbol> word = WordConversion.fromJSONDocumentToJSONSymbolWord(document);
        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();

        for (JSONSymbol symbol : word) {
            if (rand.nextInt(10) == 0) {
                wordBuilder.add(JSONSymbol.getRandomInternalOrKey(rand));
            }
            wordBuilder.add(symbol);
        }
        return wordBuilder.toWord();
    }

    protected Word<JSONSymbol> generateGibberishByRemovingSymbol(JSONObject document) {
        Word<JSONSymbol> word = WordConversion.fromJSONDocumentToJSONSymbolWord(document);
        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();

        for (JSONSymbol symbol : word) {
            if (rand.nextInt(10) != 0) {
                wordBuilder.add(symbol);
            }
        }
        return wordBuilder.toWord();
    }

    protected Word<JSONSymbol> generateGibberishByRemovingComma(JSONObject document) {
        Word<JSONSymbol> word = WordConversion.fromJSONDocumentToJSONSymbolWord(document);
        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();

        for (JSONSymbol symbol : word) {
            if (rand.nextBoolean() || !symbol.equals(JSONSymbol.toSymbol(','))) {
                wordBuilder.add(symbol);
            }
        }
        return wordBuilder.toWord();
    }

//    protected Word<JSONSymbol> generateGibberishInternalSymbols() {
//        final int nSymbols = getRandom().nextInt(MAX_NUMBER_SYMBOLS_GIBBERISH) + 1;
//        final Alphabet<JSONSymbol> internalAlphabet = alphabet.getInternalAlphabet();
//        final int sizeAlphabet = internalAlphabet.size();
//        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();
//        for (int i = 0; i < nSymbols; i++) {
//            wordBuilder.add(internalAlphabet.getSymbol(getRandom().nextInt(sizeAlphabet)));
//        }
//        return wordBuilder.toWord();
//    }

    protected Word<JSONSymbol> generateDuplicateKeyFromValid(JSONObject document) {
        Word<JSONSymbol> word = WordConversion.fromJSONDocumentToJSONSymbolWord(document);

        int numberOfKeys = 0;
        for (JSONSymbol symbol : word) {
            if (JSONSymbol.isKeySymbol(symbol)) {
//                System.out.print("key: " + symbol + "\t");
                numberOfKeys++;
            }
        }

        int keyToDuplicate = rand.nextInt(1, numberOfKeys+1);
        int depth = 0;
        boolean inserted = false;

        JSONSymbol key = null;
        WordBuilder<JSONSymbol> valueBuilder = new WordBuilder<>();
        Word<JSONSymbol> value = null;
        int keyDepth = -1;
        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();
        numberOfKeys = 0;

        for (int i = 0; i < word.size(); i++) {
            JSONSymbol symbol = word.getSymbol(i);
            if (symbol.equals(JSONSymbol.openingCurlyBraceSymbol) || symbol.equals(JSONSymbol.openingBracketSymbol)) {
                depth += 1;
            }
            if (symbol.equals(JSONSymbol.closingCurlyBraceSymbol) || symbol.equals(JSONSymbol.closingBracketSymbol)) {
                depth -= 1;
            }

            if (key != null && !inserted && depth == keyDepth-1) {
                wordBuilder.add(JSONSymbol.commaSymbol);
                wordBuilder.add(key);
                for (JSONSymbol s : value) {
                    wordBuilder.add(s);
                }
            }

            wordBuilder.add(symbol);

            if (JSONSymbol.isKeySymbol(symbol)) {
                numberOfKeys++;

                if (numberOfKeys == keyToDuplicate && key == null) {
                    keyDepth = depth;
                    key = symbol;
                    valueBuilder.add(word.getSymbol(i+1));
                    if (word.getSymbol(i+1).equals(JSONSymbol.openingCurlyBraceSymbol) ||
                            word.getSymbol(i+1).equals(JSONSymbol.openingBracketSymbol)) {
                        int d = 0;
                        for (int j = i+2; j < word.size(); j++) {
                            valueBuilder.append(word.getSymbol(j));
                            if (word.getSymbol(j).equals(JSONSymbol.openingCurlyBraceSymbol) ||
                                    word.getSymbol(j).equals(JSONSymbol.openingBracketSymbol)) {
                                d++;
                            }
                            if (word.getSymbol(j).equals(JSONSymbol.closingCurlyBraceSymbol) ||
                                    word.getSymbol(j).equals(JSONSymbol.closingBracketSymbol)) {
                                d--;
                                if (d == -1) {
                                    break;
                                }
                            }
                        }
                    }
                    value = valueBuilder.toWord();
//                    System.out.println("Duplicating key:value = " + key + " " + value + " at depth " + keyDepth);
                }
            }

            if (key != null && !inserted && depth == keyDepth) {
                if (symbol.equals(JSONSymbol.commaSymbol)) {
                    if (rand.nextInt(0, 3) == 0) {
                        wordBuilder.add(key);
                        for (JSONSymbol s : value) {
                            wordBuilder.add(s);
                        }
                        wordBuilder.add(JSONSymbol.commaSymbol);
                        inserted = true;
                    }
                }
            }
        }

//        System.out.println("Duplicate key: " + wordBuilder.toWord()
//                        + "\n\tfrom " + key + " " + value);
        return wordBuilder.toWord();
    }

    protected JSONObject generateReplacementArbitraryKey(JSONObject document) {
        if (document.keySet().contains("\\S")) {
            String addKey = JSONSymbol.getRandomKey(rand).toString().replace("\"", "").replace(":", "");
//            System.out.println(addKey);
            if (!document.keySet().contains(addKey) && rand.nextBoolean()) {
                document.put(JSONSymbol.getRandomKey(rand).toString().replace("\"", "").replace(":", ""),
                        document.get("\\S"));
                return generateReplacementArbitraryKey(document);
            }
        }
        for (String key : document.keySet()) {
            if (document.get(key) instanceof JSONObject) {
                document.put(key, generateReplacementArbitraryKey((JSONObject) document.get(key)));
            }
            else if (document.get(key) instanceof JSONArray) {
                document.put(key, generateReplacementArbitraryKey((JSONArray) document.get(key)));
            }
        }
        return document;
    }

    protected JSONArray generateReplacementArbitraryKey(JSONArray document) {
        for (int i = 0; i < document.length(); i++) {
            if (document.get(i) instanceof JSONObject) {
                document.put(i, generateReplacementArbitraryKey((JSONObject) document.get(i)));
            }
            else if (document.get(i) instanceof JSONArray) {
                document.put(i, generateReplacementArbitraryKey((JSONArray) document.get(i)));
            }
        }
        return document;
    }

    protected JSONObject generateDuplicateValueArray(JSONObject document) {
        for (String key : document.keySet()) {
            if (document.get(key) instanceof JSONObject) {
                document.put(key, generateDuplicateValueArray((JSONObject) document.get(key)));
            }
            else if (document.get(key) instanceof JSONArray) {
                document.put(key, generateDuplicateValueArray((JSONArray) document.get(key)));
            }
        }
        return document;
    }

    protected JSONArray generateDuplicateValueArray(JSONArray document) {
        int max_size = document.length()*2;
        for (int i = 0; i < max_size && i < document.length(); i++) {
            if (document.get(i) instanceof JSONObject) {
                document.put(i, generateReplacementArbitraryKey((JSONObject) document.get(i)));
            }
            else if (document.get(i) instanceof JSONArray) {
                document.put(i, generateReplacementArbitraryKey((JSONArray) document.get(i)));
            }
            if (rand.nextBoolean()) {
                document.put(document.get(i));
            }
        }
        return document;
    }

    protected DefaultQuery<JSONSymbol, Boolean> checkDocument(A hypothesis, JSONObject document) {
        boolean correctForSchema;
        try {
            correctForSchema = validator.validate(schema, document);
        } catch (JSONSchemaException e) {
            e.printStackTrace(System.err);
            return null;
        }
        Word<JSONSymbol> word = WordConversion.fromJSONDocumentToJSONSymbolWord(document, shouldShuffleKeys(), rand);
//        System.out.println("Checking word: " + word + " over alphabet:\n " + alphabet);
        boolean correctForHypo = hypothesis.accepts(word);

        if (correctForSchema != correctForHypo) {
            return new DefaultQuery<>(word, correctForSchema);
        }
        return null;
    }

    protected DefaultQuery<JSONSymbol, Boolean> checkWord(A hypothesis, Word<JSONSymbol> word) {
        String string = WordConversion.fromJSONSymbolWordToString(word);
        string = Utils.escapeSymbolsForJSON(string);
        if (!Utils.validWord(string) || !Utils.validWordObject(word)) {
            // Since constructing the JSON object might resolve the reason why the word is
            // invalid (such as missing quotes around a key), we handle this case explicitly
            if (hypothesis.accepts(word)) {
                return new DefaultQuery<>(word, false);
            }
        } else {
            JSONObject document = null;
            try {
                document = new JSONObject(string);
            } catch (JSONException e) {
                if (hypothesis.accepts(word)) {
                    return new DefaultQuery<>(word, false);
                }
            }

            if (document != null) {
                boolean correctForSchema;
                try {
                    correctForSchema = validator.validate(schema, document);
                } catch (JSONSchemaException e) {
                    e.printStackTrace(System.err);
                    return null;
                }
                boolean correctForHypo = hypothesis.accepts(word);

                if (correctForSchema != correctForHypo) {
                    return new DefaultQuery<>(word, correctForSchema);
                }
            }
        }
        return null;
    }

    protected DefaultQuery<JSONSymbol, Boolean> findCounterexampleFromValid(A hypothesis, JSONObject document) {
        if (Thread.interrupted()) {
            Thread.currentThread().interrupt();
            return null;
        }

//            System.out.println("Trying finding CX on valid: " + document);

        DefaultQuery<JSONSymbol, Boolean> query = checkDocument(hypothesis, document);
        if (query != null) {
            return query;
        }

        if (document.toString().length() > 2) {
            for (int i = 0; i < numberTests / numberValid / 10 + 1; i++) {
                JSONObject variant_document = generateReplacementArbitraryKey(new JSONObject(document.toString()));
                query = checkDocument(hypothesis, variant_document);
                if (query != null) {
                    return query;
                }

                query = checkWord(hypothesis, generateDuplicateKeyFromValid(variant_document));
                if (query != null) {
                    return query;
                }

                variant_document = generateDuplicateValueArray(new JSONObject(document.toString()));
                query = checkDocument(hypothesis, variant_document);
                if (query != null) {
                    return query;
                }

                query = checkWord(hypothesis, generateGibberishByAddingSymbol(variant_document));
                if (query != null) {
                    return query;
                }

                query = checkWord(hypothesis, generateGibberishByRemovingSymbol(variant_document));
                if (query != null) {
                    return query;
                }

                query = checkWord(hypothesis, generateGibberishByRemovingComma(variant_document));
                if (query != null) {
                    return query;
                }

//                if (hypothesis.accepts(word)) {/*
//                 *  We are suppose to use:
//                 *      query = checkDocument(hypothesis, document)
//                 *  but this doesn't work. Sometimes, a JSON is accepted by the schema, even if it is not a valid JSON document
//                 *                          (for instance, when it contains an array [ true false ] without comma).
//                 *  We should check if the problem comes from the validator.
//                fixed ?
//                 */
//                    System.out.println("Found gibberish cx:" + word);
//                    return new DefaultQuery<>(word, false);
//                }
//
            }
        }

        return null;
    }

    protected DefaultQuery<JSONSymbol, Boolean> findCounterexampleFromInvalid(A hypothesis, JSONObject document) {
        if (Thread.interrupted()) {
            Thread.currentThread().interrupt();
            return null;
        }
        DefaultQuery<JSONSymbol, Boolean> query = checkDocument(hypothesis, document);
        if (query != null) {
            return query;
        }

        query = checkWord(hypothesis, generateGibberish());
//        System.out.println(generateGibberish());
//        if (query != null) {
        return query;
//        }

//        query = checkWord(hypothesis, generateGibberishInternalSymbols());
//        return query;
    }

    protected void resetTime() {
        timeLastEQ = System.nanoTime();
    }

    protected void setTime() {
        timeLastEQ = System.nanoTime() - timeLastEQ;
    }

    public float getTime() {
        return ((float) timeLastEQ) / 1_000_000_000;
    }
}
