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
import be.ac.umons.jsonschematools.generator.exploration.DefaultExplorationGenerator;
import be.ac.umons.jsonschematools.generator.exploration.ExplorationGenerator;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONObject;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.JSONutils.WordConversion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * Base class for conformance testing-based equivalence oracles using an
 * exhaustive JSON document generator.
 * 
 * <p>
 * When performing an equivalence check, the following tests are performed, in
 * this order:
 * <ol>
 * <li>Is there a valid document that is rejected by the hypothesis?</li>
 * <li>Is there an invalid document that is accepted by the hypothesis?</li>
 * <li>Is there a gibberish word that is accepted by the hypothesis?</li>
 * </ol>
 * </p>
 * 
 * @param <A> Automaton type
 * @author Gaëtan Staquet
 */
abstract class AbstractJSONConformanceExhaustive<A extends DeterministicAcceptorTS<?, JSONSymbol>>
        extends AbstractJSONConformance<A> implements EquivalenceOracle<A, JSONSymbol, Boolean> {

//    private final static LearnLogger LOGGER = LearnLogger.getLogger(AbstractExplorationJSONConformance.class); TODO

    private final ExplorationGenerator generator;
    private Iterator<JSONObject> iteratorValidDocuments = null;
    private Iterator<JSONObject> iteratorInvalidDocuments = null;
    private int numberGeneratedInvalidDocuments = 0;
    private int numberGeneratedValidDocuments = 0;
    private int maxDocumentDepth;

    protected AbstractJSONConformanceExhaustive(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                                JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet,
                                                int maxDocumentDepth) {
        super(numberTests, canGenerateInvalid, maxProperties, maxItems,
                schema, random, shuffleKeys, alphabet);
        this.generator = new DefaultExplorationGenerator(maxProperties, maxItems);
        this.maxDocumentDepth = maxDocumentDepth;
        setMaximalDocumentDepth(this.maxDocumentDepth);
        numberValid = 0;
        while (iteratorValidDocuments.hasNext() && continueValidGeneration()) {
            iteratorValidDocuments.next();
            numberValid++;
        }
        numberGeneratedValidDocuments = 0;
        System.out.println("numberValid: " + numberValid);
    }

    @Override
    public @Nullable DefaultQuery<JSONSymbol, Boolean> findCounterExample(A hypo,
                                                                          Collection<? extends JSONSymbol> inputs) {
        return findCounterExample(hypo);
    }

    private boolean continueInvalidGeneration() {
        if (numberTests() == -1) {
            return true;
        }
        if (numberGeneratedInvalidDocuments % 500 == 0 && numberGeneratedInvalidDocuments > 0) {
            System.out.println("Number of invalid tried: " + numberGeneratedInvalidDocuments);
        }
        return numberGeneratedInvalidDocuments++ < numberTests();
    }

    private boolean continueValidGeneration() {
        if (numberTests() == -1) {
            return true;
        }
        if (numberGeneratedValidDocuments % 500 == 0 && numberGeneratedValidDocuments > 0) {
            System.out.println("Number of valid tried: " + numberGeneratedValidDocuments);
        }
        return numberGeneratedValidDocuments++ < numberTests();
    }

    protected int numberGibberish() {
        if (numberTests() == -1) {
            return 10;
        } else {
            return numberTests();
        }
    }

    @Nullable
    protected DefaultQuery<JSONSymbol, Boolean> findCounterExample(A hypothesis) {
        numberGeneratedInvalidDocuments = 0;
        setMaximalDocumentDepth(maxDocumentDepth);

        resetTime();

        while (
                iteratorValidDocuments.hasNext() && continueValidGeneration()
            ||   iteratorInvalidDocuments.hasNext() && continueInvalidGeneration()
        ) {
            if (iteratorValidDocuments.hasNext()) {
                JSONObject document = iteratorValidDocuments.next();
                DefaultQuery<JSONSymbol, Boolean> query = findCounterexampleFromValid(hypothesis, document);
                if (query != null) {
                    return query;
                }
            }
            if (iteratorInvalidDocuments.hasNext()) {
                JSONObject document = iteratorInvalidDocuments.next();
                DefaultQuery<JSONSymbol, Boolean> query = findCounterexampleFromInvalid(hypothesis, document);
                if (query != null) {
                    return query;
                }
            }
        }
//        LOGGER.info("Valid documents exhausted");

        setTime();

        return null;
    }

    protected void setMaximalDocumentDepth(int maxDocumentDepth) {
        this.iteratorValidDocuments = generator.createIterator(getSchema(), maxDocumentDepth, false);
        if (canGenerateInvalid()) {
            this.iteratorInvalidDocuments = generator.createIterator(getSchema(), maxDocumentDepth, true);
        }
    }
}
