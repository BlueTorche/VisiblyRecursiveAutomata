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
import de.learnlib.oracle.EquivalenceOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.vra.VRA;

import java.util.Random;

public class VRAJSONEquivalenceOracleExhaustive
        extends AbstractJSONConformanceExhaustive<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>>
        implements EquivalenceOracle<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>, JSONSymbol, Boolean> {

    public VRAJSONEquivalenceOracleExhaustive(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                              JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet,
                                              int maxDocumentDepth) {
        super(numberTests, canGenerateInvalid, maxProperties, maxItems,
                schema, random, shuffleKeys, alphabet,
                maxDocumentDepth);
    }
}
