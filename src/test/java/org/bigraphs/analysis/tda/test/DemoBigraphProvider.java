/*
 * Copyright (c) 2026 Bigraph Toolkit Suite Developers
 * Main Developer: Dominik Grzelak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bigraphs.analysis.tda.test;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;

import org.bigraphs.framework.core.alg.generators.PureBigraphGenerator;
import org.bigraphs.framework.core.datatypes.FiniteOrdinal;
import org.bigraphs.framework.core.datatypes.StringTypedName;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;

/**
 * This class provides test data for generating random {@link PureBigraph} instances and
 * predefined {@link DynamicSignature} instances.
 * <p>
 * The provider supports generating bigraphs with multiple or single roots, configurable
 * numbers of nodes and sites, and different link configurations.
 * It also provides signatures for common demo scenarios.
 *
 * @author Dominik Grzelak
 * @see PureBigraph
 * @see DynamicSignature
 * @see PureBigraphGenerator
 */
public class DemoBigraphProvider {

    public PureBigraph getRandomBigraphMultipleRoots(int root, int nodes) {
        return new PureBigraphGenerator(createRandomSignature()).generate(root, nodes, 1.f);
    }

    public PureBigraph getRandomBigraphMultipleRoots(int root, int nodes, int sites, DynamicSignature sig) {
        return new PureBigraphGenerator(sig).generate(root, nodes, sites, 1.f);
    }

    public PureBigraph getRandomBigraphSingleRoot(int n, int s) {
        return new PureBigraphGenerator(createRandomSignature()).generate(1, n, s, 1.f);
    }

    public PureBigraph getRandomPlaceGraphSingleRoot(int n, int s, DynamicSignature signature) {
        return new PureBigraphGenerator(signature).generate(1, n, s, 0.f);
    }

    public PureBigraph getRandomBigraphSingleRoot(int n, int s, DynamicSignature signature) {
        return new PureBigraphGenerator(signature).generate(1, n, s, 1.f);
    }

    public PureBigraph getRandomBigraphSingleRootOnlyOuternames(int n, int s) {
        return new PureBigraphGenerator(createRandomSignature()).generate(1, n, s, 1.f, 1.f, 0.f);
    }

    public PureBigraph getRandomBigraphSingleRootOnlyEdges(int n, int s) {
        return new PureBigraphGenerator(createRandomSignature()).generate(1, n, s, 1.f, 0.f, 1.f);
    }

    public DynamicSignature createRandomSignature() {
        DynamicSignatureBuilder signatureBuilder = pureSignatureBuilder();
        signatureBuilder
                .newControl().identifier(StringTypedName.of("Printer")).arity(FiniteOrdinal.ofInteger(2)).assign()
                .newControl().identifier(StringTypedName.of("User")).arity(FiniteOrdinal.ofInteger(1)).assign()
                .newControl().identifier(StringTypedName.of("Room")).arity(FiniteOrdinal.ofInteger(1)).assign()
                .newControl().identifier(StringTypedName.of("Spool")).arity(FiniteOrdinal.ofInteger(1)).assign()
                .newControl().identifier(StringTypedName.of("Computer")).arity(FiniteOrdinal.ofInteger(1)).assign()
                .newControl().identifier(StringTypedName.of("Job")).arity(FiniteOrdinal.ofInteger(0)).assign();

        return signatureBuilder.create();
    }

    public DynamicSignature createAlphabetSignature() {
        DynamicSignatureBuilder defaultBuilder = pureSignatureBuilder();
        defaultBuilder
                .newControl().identifier(StringTypedName.of("A")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("B")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("C")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("D")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("E")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("F")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("G")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("H")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("I")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("J")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("K")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("Q")).arity(FiniteOrdinal.ofInteger(5)).assign()
                .newControl().identifier(StringTypedName.of("R")).arity(FiniteOrdinal.ofInteger(5)).assign()
        ;

        return defaultBuilder.create();
    }

    public DynamicSignature createABCSignatureArityZero() {
        DynamicSignatureBuilder defaultBuilder = pureSignatureBuilder();
        defaultBuilder
                .newControl().identifier(StringTypedName.of("A")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("B")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("C")).arity(FiniteOrdinal.ofInteger(0)).assign()
        ;

        return defaultBuilder.create();
    }
}
