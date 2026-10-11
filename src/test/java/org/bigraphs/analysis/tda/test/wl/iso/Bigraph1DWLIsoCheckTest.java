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
package org.bigraphs.analysis.tda.test.wl.iso;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.wl.BWLIsomorphismChecker;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.junit.jupiter.api.Disabled;
import org.testng.annotations.Test;

/**
 * @author Dominik Grzelak
 */
public class Bigraph1DWLIsoCheckTest implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/wl-iso-test/";

    DemoBigraphProvider provider = new DemoBigraphProvider();

    @Test
    public void test_twoSymmetricBigraphs() {
        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig);
        b.root()
                .child("B").down().child("A").child("A").top()
                .child("A").down().child("B").child("B").top()
                .child("A").down().child("B").child("B").top()
        ;
        PureBigraph bigraphA = b.create();
        toPNG(bigraphA, "wl-pg-bigraph_symmetric_A", DUMP_PATH);

        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(sig);
        b2.root()
                .child("A").down().child("B").child("B").top()
                .child("B").down().child("A").child("A").top()
                .child("B").down().child("A").child("A").top()
        ;
        PureBigraph bigraphB = b2.create();
        toPNG(bigraphB, "wl-pg-bigraph_symmetric_B", DUMP_PATH);

        BWLIsomorphismChecker check = new BWLIsomorphismChecker(bigraphA, bigraphB);
        boolean areIsomorphic = check.isoCheckByPlaceGraph();
        System.out.println("Place Graphs are isomorphic: " + areIsomorphic);
        assertFalse(areIsomorphic);
        areIsomorphic = check.isoCheckByLinkGraph();
        System.out.println("Link Graphs are isomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
        areIsomorphic = check.isoCheck();
        System.out.println("Bigraphs are isomorphic: " + areIsomorphic);
        assertFalse(areIsomorphic);

    }

    @Test
    public void test_twoSamePlaceGraphs() throws Exception {
        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig);
        BigraphEntity.InnerName tmp = b.createInner("tmp");
        b.root()
                .child("A")
                .child("B").down().child("C").linkInner(tmp).top();
        b.root().child("A").child("B").down().child("D").linkInner(tmp).top();
        b.closeInner(tmp);
        PureBigraph bigraphA = b.create();
        toPNG(bigraphA, "wl-pg-A", DUMP_PATH);

        // same bigraph as above, but only first-level nodes under roots swapped
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(sig);
        BigraphEntity.InnerName tmp2 = b2.createInner("tmp");
        b2.root() // here we swap the two nodes under the first root
                .child("B").down().child("C").linkInner(tmp2).top()
                .child("A").top();
        b2.root().child("B").down().child("D").linkInner(tmp2).up().child("A");
        b2.closeInner(tmp2);
        PureBigraph bigraphB = b2.create();
        toPNG(bigraphB, "wl-pg-B", DUMP_PATH);

        BWLIsomorphismChecker check = new BWLIsomorphismChecker(bigraphA, bigraphB);
        boolean areIsomorphic = check.isoCheckByPlaceGraph();
        System.out.println("PGs areIsomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
        areIsomorphic = check.isoCheckByLinkGraph();
        System.out.println("LGs areIsomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
        areIsomorphic = check.isoCheck();
        System.out.println("Bigraphs areIsomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
    }

    @Test
    public void test_twoSameBigraphs() throws Exception {
        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig);
        BigraphEntity.InnerName tmp = b.createInner("tmp");
        b.root()
                .child("A").linkOuter("Y").connectByEdge("D", "D", "D").top()
                .child("B").down().child("C").linkInner(tmp).top();
        b.root().child("A").child("B").down().child("D").linkInner(tmp).top();
//        b.closeInner(tmp);
        PureBigraph bigraphA = b.create();
        toPNG(bigraphA, "wl-big-A", DUMP_PATH);

        // same bigraph as above, but only first-level nodes under roots swapped
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(sig);
        BigraphEntity.InnerName tmp2 = b2.createInner("tmp");
        b2.root() // here we swap the two nodes under the first root
                .child("B").down().child("C").linkInner(tmp2).top()
                .child("A").linkOuter("Y").connectByEdge("D", "D", "D").top();
        b2.root().child("B").down().child("D").linkInner(tmp2).up().child("A")
        ;
//        b2.closeInner(tmp2); // <- this makes them non-isomorphic
        PureBigraph bigraphB = b2.create();
        toPNG(bigraphB, "wl-big-B", DUMP_PATH);

        BWLIsomorphismChecker check = new BWLIsomorphismChecker(bigraphA, bigraphB);
        boolean areIsomorphic = check.isoCheckByPlaceGraph();
        System.out.println("PGs areIsomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
        areIsomorphic = check.isoCheckByLinkGraph();
        System.out.println("LGs areIsomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
        areIsomorphic = check.isoCheck();
        System.out.println("Bigraphs areIsomorphic: " + areIsomorphic);
        assertTrue(areIsomorphic);
    }

    /**
     * One root, one site and two nodes.
     * Linking will not be considered for the check here.
     * Should terminate in a reasonable amount of time due to the low combinatorial power.
     */
    @Test
    @Disabled
    public void test_findSameRandomBigraphs() {
        int n = 3;
        PureBigraph bigraph1 = provider.getRandomBigraphSingleRoot(n, 1);
        toPNG(bigraph1, "wl-random-1", DUMP_PATH);
        boolean areIsomorphic = false;
        while (true) {
            PureBigraph bigraph2 = provider.getRandomBigraphSingleRoot(n, 1);
            BWLIsomorphismChecker check = new BWLIsomorphismChecker(bigraph1, bigraph2);
//            areIsomorphic = check.isoCheckByPlaceGraph();
            areIsomorphic = check.isoCheck();
            if (areIsomorphic) {
                toPNG(bigraph2, "wl-random-2", DUMP_PATH);
                System.out.println("areIsomorphic: " + areIsomorphic);
                break;
            }
        }
        assertTrue(areIsomorphic);
    }


    /**
     * One root, one site and two nodes, and only outer names.
     * Can take very long for n >= 4
     */
    @Test
    @Disabled
    public void test_Iso_randomBigraphs() {
        int n = 3;
        PureBigraph bigraph1 = provider.getRandomBigraphSingleRootOnlyOuternames(n, 1);
        toPNG(bigraph1, "wl-big-random-1", DUMP_PATH);
        boolean areIsomorphic = false;
        while (true) {
            PureBigraph bigraph2 = provider.getRandomBigraphSingleRootOnlyOuternames(n, 1);
            toPNG(bigraph2, "wl-big-random-2", DUMP_PATH);
            BWLIsomorphismChecker check = new BWLIsomorphismChecker(bigraph1, bigraph2);
            areIsomorphic = check.isoCheck();
            if (areIsomorphic) {
                System.out.println("areIsomorphic: " + areIsomorphic);
                break;
            }
        }
        assertTrue(areIsomorphic);
    }
}
