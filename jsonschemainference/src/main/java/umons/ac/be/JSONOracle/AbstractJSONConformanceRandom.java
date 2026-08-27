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
import be.ac.umons.jsonschematools.generator.random.DefaultRandomGenerator;
import be.ac.umons.jsonschematools.generator.random.RandomGenerator;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONObject;
import umons.ac.be.JSONutils.JSONSymbol;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;


/**
 * Base class for (partial) equivalence queries using a random generator.
 *
 * <p>
 * It generates a fixed number of JSON documents (up to the fixed maximal depth)
 * and checks whether the hypothesis and the classical validator agrees on their
 * validity.
 * </p>
 *
 * @author Gaëtan Staquet
 */
public abstract class AbstractJSONConformanceRandom<A extends DeterministicAcceptorTS<?, JSONSymbol>>
        extends AbstractJSONConformance<A> implements EquivalenceOracle<A, JSONSymbol, Boolean> {
    private final RandomGenerator generator;
    private final Iterator<JSONObject> validIterator;
    private final Iterator<JSONObject> invalidIterator;
    private final int maxDocumentDepth;
    private final Collection<JSONObject> documentsToTest;

    protected AbstractJSONConformanceRandom(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                            JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet,
                                            int maxDocumentDepth, Collection<JSONObject> documentsToTest) {
        super(numberTests, canGenerateInvalid, maxProperties, maxItems, schema, random, shuffleKeys, alphabet);
        this.generator = new DefaultRandomGenerator(maxProperties, maxItems);
        this.validIterator = generator.createIterator(schema, -1, canGenerateInvalid, random);
        this.invalidIterator = generator.createIterator(schema, -1, canGenerateInvalid, random);
        this.maxDocumentDepth = maxDocumentDepth;
        this.documentsToTest = documentsToTest;
        this.numberValid = numberTests;
        // TODO: the main difference with the Exhaustive is the generator (if we exclude its size)
        //  We should investigate what's the difference and see if it is necessary to have two class
    }

    @Override
    public @Nullable DefaultQuery<JSONSymbol, Boolean> findCounterExample(A hypo,
                                                                          Collection<? extends JSONSymbol> inputs) {
        return findCounterExample(hypo);
    }

    protected DefaultQuery<JSONSymbol, Boolean> findCounterExample(A hypothesis) {
        for (JSONObject document : documentsToTest) {
            DefaultQuery<JSONSymbol, Boolean> query = checkDocument(hypothesis, document);
            if (query != null) {
                return query;
            }
        }

        for (int i = 0; i < numberTests(); i++) {
            if (i % 500 == 0 && i > 0) {
                System.out.println(i + " iteration of searching CX out of " + numberTests());
            }

            JSONObject document = validIterator.next();
            DefaultQuery<JSONSymbol, Boolean> query = findCounterexampleFromValid(hypothesis, document);
            if (query != null) {
                return query;
            }

            if (canGenerateInvalid()) {
                document = invalidIterator.next();
                query = findCounterexampleFromInvalid(hypothesis, document);
                if (query != null) {
                    return query;
                }
            }

        }

        return null;
    }



//    protected @Nullable DefaultQuery<JSONSymbol, Boolean> findCounterExample(A hypothesis) {
//        for (JSONObject document : documentsToTest) {
//            DefaultQuery<JSONSymbol, Boolean> query = checkDocument(hypothesis, document);
//            if (query != null) {
//                return query;
//            }
//        }
//
//        for (int maxDepth = 0; maxDepth <= maxDocumentDepth; maxDepth++) {
//            if (Thread.interrupted()) {
//                Thread.currentThread().interrupt();
//                return null;
//            }
//
//            DefaultQuery<JSONSymbol, Boolean> counterexample = findCounterExample(hypothesis, maxDepth);
//            if (counterexample != null) {
//                return counterexample;
//            }
//        }
//        return null;
//    }
}
