package umons.ac.be.JSONutils;

import static umons.ac.be.JSONutils.JSONSymbol.toSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

/**
 * Utility functions to convert a word of JSON symbols to a regular string, and
 * vice-versa.
 *
 * @author Gaëtan Staquet
 */
public class WordConversion {
    public static Word<JSONSymbol> fromJSONDocumentToJSONSymbolWord(JSONObject document) {
        return fromJSONDocumentToJSONSymbolWord(document, false, new Random());
    }

    public static Word<JSONSymbol> fromJSONDocumentToJSONSymbolWord(JSONObject document, boolean shuffleKeys,
                                                                    Random rand) {
        WordBuilder<JSONSymbol> wordBuilder = new WordBuilder<>();
        wordBuilder.add(toSymbol("{"));
        fromJSONObjectToJSONWord(document, shuffleKeys, rand, wordBuilder);
        wordBuilder.add(toSymbol("}"));
        return wordBuilder.toWord();
    }

    private static void fromJSONObjectToJSONWord(JSONObject object, boolean shuffleKeys, Random rand,
                                                 WordBuilder<JSONSymbol> wordBuilder) {
        List<String> keys = new ArrayList<>(object.keySet());
        keys.sort(String.CASE_INSENSITIVE_ORDER);
        if (shuffleKeys) {
            Collections.shuffle(keys, rand);
        }
        boolean first = true;
//        System.out.println(object.toString());
        for (String key : keys) {
            if (!first) {
                wordBuilder.add(JSONSymbol.commaSymbol);
            }
            first = false;
            wordBuilder.add(toSymbol("\"" + key + "\":"));

            Object o = object.get(key);
            if (o instanceof JSONObject) {
                wordBuilder.add(toSymbol("{"));
                fromJSONObjectToJSONWord((JSONObject) o, shuffleKeys, rand, wordBuilder);
                wordBuilder.add(toSymbol("}"));
            } else if (o instanceof JSONArray) {
                wordBuilder.add(toSymbol("["));
                fromJSONArrayToJSONWord((JSONArray) o, shuffleKeys, rand, wordBuilder);
                wordBuilder.add(toSymbol("]"));
            } else if (o instanceof Boolean) {
                wordBuilder.add(toSymbol(o.toString()));
            } else {
                if (Objects.equals(o, JSONObject.NULL)) {
                    wordBuilder.add(toSymbol(o.toString()));
                } else {
                    wordBuilder.add(toSymbol("\"" + o.toString() + "\""));
                }
            }
        }
    }

    private static void fromJSONArrayToJSONWord(JSONArray array, boolean shuffleKeys, Random rand,
                                                WordBuilder<JSONSymbol> wordBuilder) {
        boolean first = true;
        for (Object o : array) {
            if (!first) {
                wordBuilder.add(JSONSymbol.commaSymbol);
            }
            first = false;
            if (o instanceof JSONObject) {
                wordBuilder.add(toSymbol("{"));
                fromJSONObjectToJSONWord((JSONObject) o, shuffleKeys, rand, wordBuilder);
                wordBuilder.add(toSymbol("}"));
            } else if (o instanceof JSONArray) {
                wordBuilder.add(toSymbol("["));
                fromJSONArrayToJSONWord((JSONArray) o, shuffleKeys, rand, wordBuilder);
                wordBuilder.add(toSymbol("]"));
            } else if (o instanceof Boolean) {
                wordBuilder.add(toSymbol(o.toString()));
            } else if (Objects.equals(o, JSONObject.NULL)) {
                wordBuilder.add(toSymbol(o.toString()));
            } else {
                wordBuilder.add(toSymbol("\"" + o.toString() + "\""));
            }
        }
    }

    public static String fromJSONSymbolWordToString(Word<JSONSymbol> word) {
        StringBuilder stringBuilder = new StringBuilder(word.size());
        for (JSONSymbol symbol : word) {
            stringBuilder.append(symbol.toString());
        }
        return stringBuilder.toString();
//        return word.toString();
    }

    public static int countNumberOfSymbols(JSONObject object) {
        int total = 1 + object.keySet().size()*2;
        for (String key : object.keySet()) {
            if (object.get(key) instanceof JSONObject) {
                total += countNumberOfSymbols((JSONObject) object.get(key));
            }
            else if (object.get(key) instanceof JSONArray) {
                total += countNumberOfSymbols((JSONArray) object.get(key));
            }
            else {
                total += 1;
            }
        }
        if (object.isEmpty()) {
            total += 1;
        }
        return total;
    }

    public static int countNumberOfSymbols(JSONArray array) {
        int total = 1 + array.length()*2;
        for (Object o : array) {
            if (o instanceof JSONObject) {
                total += countNumberOfSymbols((JSONObject) o);
            }
            else if (o instanceof JSONArray) {
                total += countNumberOfSymbols((JSONArray) o);
            }
            else {
                total += 1;
            }
        }
        if (array.isEmpty()) {
            total += 1;
        }
        return total;
    }
}
