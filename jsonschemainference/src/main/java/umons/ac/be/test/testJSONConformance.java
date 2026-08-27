package umons.ac.be.test;

import be.ac.umons.jsonschematools.JSONSchema;
import de.learnlib.oracle.MembershipOracle;
import jdk.jshell.execution.Util;
import net.automatalib.word.Word;
import org.json.JSONException;
import org.json.JSONObject;
import umons.ac.be.JSONOracle.JSONMembershipOracle;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.JSONutils.Utils;
import umons.ac.be.JSONutils.WordConversion;

import java.nio.file.Path;
import java.nio.file.Paths;

public class testJSONConformance {
    public static void main(String[] args) {
//        testMembershipOracle();
        testValidWord();
    }

    private static void testValidWord() {

        Word<JSONSymbol> word = Word.fromSymbols(
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"\\S\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.closingCurlyBraceSymbol
        );

        System.out.println(Utils.validWordObject(word));


        word = Word.fromSymbols(
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"\\S\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("true"),
                JSONSymbol.toSymbol("false"),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.closingCurlyBraceSymbol
        );

        System.out.println(Utils.validWordObject(word));
    }

    private static void testMembershipOracle() {
        String schema = "vscode"; // new String[]{"recursiveList", "basicTypes", "vscode", "vim", "proxies", "codecov"}[schemaIndex];
        Path filePath = Paths.get(
                "C:\\Users\\dubru\\IdeaProjects\\ValidatingJSONDocumentsWithLearnedVPA-main\\schemas\\benchmarks\\"
                        + schema + "\\" + schema + ".json"
        );

        final JSONSchema jsonSchema = testJSONLearning.getSchema(filePath);
        final MembershipOracle<JSONSymbol, Boolean> membershipOracle = new JSONMembershipOracle(jsonSchema);

        Word<JSONSymbol> word1 = Word.fromSymbols(
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"\\S\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.closingCurlyBraceSymbol
        );

        System.out.println(word1);

        String string1 = WordConversion.fromJSONSymbolWordToString(word1);
        string1 = Utils.escapeSymbolsForJSON(string1);
        System.out.println(string1);
        JSONObject json1 = null;
        try {
            json1 = new JSONObject(string1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println(json1);
        System.out.println(membershipOracle.answerQuery(word1));

        System.out.println(WordConversion.fromJSONDocumentToJSONSymbolWord(json1));
        System.out.println(word1.equals(WordConversion.fromJSONDocumentToJSONSymbolWord(json1)));


        Word<JSONSymbol> word2 = Word.fromSymbols(
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"\\S\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.openingCurlyBraceSymbol,
                JSONSymbol.toSymbol("\"body\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"description\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"prefix\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.commaSymbol,
                JSONSymbol.toSymbol("\"scope\":"),
                JSONSymbol.toSymbol("\"\\S\""),
                JSONSymbol.closingCurlyBraceSymbol,
                JSONSymbol.closingCurlyBraceSymbol
        );


        System.out.println();
        System.out.println(word2);

        String string2 = WordConversion.fromJSONSymbolWordToString(word2);
        string2 = Utils.escapeSymbolsForJSON(string2);
        System.out.println(string2);
        JSONObject json2 = null;
        try {
            json2 = new JSONObject(string2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println(json2);
        System.out.println(membershipOracle.answerQuery(word2));

        System.out.println(WordConversion.fromJSONDocumentToJSONSymbolWord(json2));
        System.out.println(word2.equals(WordConversion.fromJSONDocumentToJSONSymbolWord(json2)));

        System.out.println();
        System.out.println(json1.similar(json2));
        System.out.println(WordConversion.fromJSONDocumentToJSONSymbolWord(json1).equals(WordConversion.fromJSONDocumentToJSONSymbolWord(json2)));
    }
}
