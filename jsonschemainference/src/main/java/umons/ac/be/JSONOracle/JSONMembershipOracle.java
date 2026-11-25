package umons.ac.be.JSONOracle;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import net.automatalib.word.Word;
import be.ac.umons.jsonschematools.validator.DefaultValidator;
import be.ac.umons.jsonschematools.validator.Validator;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.JSONutils.Utils;
import umons.ac.be.JSONutils.WordConversion;
import umons.ac.be.oracle.AbstractCounterMembershipOracle;
import umons.ac.be.oracle.CounterMembershipOracle;


/**
 * Membership oracle for JSON documents.
 *
 * It checks whether a provided document is accepted by the JSON Schema.
 *
 * @author Gaëtan Staquet
 */
public class JSONMembershipOracle extends AbstractCounterMembershipOracle<JSONSymbol> {
    private final JSONSchema schema;
    private final Validator validator;

    private final HashMap<Word<JSONSymbol>, Boolean> answeredQuery = new HashMap<>();

    public JSONMembershipOracle(JSONSchema schema) {
        this.schema = schema;
        this.validator = new DefaultValidator();
    }

    @Override
    public Boolean getAnswer(Word<JSONSymbol> input) {
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
            // answeredQuery.put(input, false);
            return false;
        }
    }

    @Override
    public Boolean answerQuery(Word<JSONSymbol> prefix, Word<JSONSymbol> suffix) {
        return answerQuery(prefix.concat(suffix));
    }
}
