package umons.ac.be.JSONOracle;

import java.util.HashMap;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import de.learnlib.oracle.SingleQueryOracle;
import net.automatalib.word.Word;
import be.ac.umons.jsonschematools.validator.DefaultValidator;
import be.ac.umons.jsonschematools.validator.Validator;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.JSONutils.Utils;
import umons.ac.be.JSONutils.WordConversion;


/**
 * Membership oracle for JSON documents.
 *
 * It checks whether a provided document is accepted by the JSON Schema.
 *
 * @author Gaëtan Staquet
 */
public class JSONMembershipOracle implements SingleQueryOracle.SingleQueryOracleDFA<JSONSymbol> {

    private final JSONSchema schema;
    private final Validator validator;
    private int numberOfMQ = 0;

    private final HashMap<Word<JSONSymbol>, Boolean> answeredQuery = new HashMap<>();

    public JSONMembershipOracle(JSONSchema schema) {
        this.schema = schema;
        this.validator = new DefaultValidator();
    }

    @Override
    public Boolean answerQuery(Word<JSONSymbol> input) {
//        if (answeredQuery.containsKey(input)) {
//            return answeredQuery.get(input);
//        }
        numberOfMQ++;

        String string = WordConversion.fromJSONSymbolWordToString(input);
        if (!Utils.validWord(string) || !Utils.validWordObject(input)) {
//            answeredQuery.put(input, false);
            return false;
        }
        string = Utils.escapeSymbolsForJSON(string);
        JSONObject json;
        try {
            json = new JSONObject(string);
        } catch (JSONException e) {
//            answeredQuery.put(input, false);
            return false;
        }

        // Assert good order of key
        if (!input.equals(WordConversion.fromJSONDocumentToJSONSymbolWord(json))) {
//            System.out.println("Bad order of key:" + input + " --- " + WordConversion.fromJSONDocumentToJSONSymbolWord(json));
//            answeredQuery.put(input, false);
            return false;
        }

        final Word<JSONSymbol> wordFromDocument = WordConversion.fromJSONDocumentToJSONSymbolWord(json);
        if (!wordFromDocument.equals(input)) {
//            answeredQuery.put(input, false);
            return false;
        }

        try {
            boolean answer = validator.validate(schema, json);
//            answeredQuery.put(input, answer);
            return answer;
        } catch (JSONSchemaException e) {
            e.printStackTrace(System.err);
            answeredQuery.put(input, false);
            return false;
        }
    }

    public int getNumberOfMQ() {
        return numberOfMQ;
    }

    @Override
    public Boolean answerQuery(Word<JSONSymbol> prefix, Word<JSONSymbol> suffix) {
        return answerQuery(prefix.concat(suffix));
    }
}
