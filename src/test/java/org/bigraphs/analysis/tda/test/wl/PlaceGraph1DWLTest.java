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
package org.bigraphs.analysis.tda.test.wl;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;

import com.google.common.collect.Multiset;
import java.util.Map;
import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.wl.AbstractWLColorRefinementProcedure;
import org.bigraphs.analysis.tda.wl.impl.PlaceGraphWLColorRefinementImpl;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.junit.jupiter.api.Test;

/**
 * @author Dominik Grzelak
 */
public class PlaceGraph1DWLTest implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/wl-pg/";

    DemoBigraphProvider provider = new DemoBigraphProvider();

    @Test
    public void test_WL_for_random_placeGraph() {
        int n = 10;
        PureBigraph bigraph = provider.getRandomBigraphSingleRoot(n, 0);
        toPNG(bigraph, "wl-pg-big-1", DUMP_PATH);
        PlaceGraphWLColorRefinementImpl wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraph);
        AbstractWLColorRefinementProcedure.WLResult h = wlPlaceGraph.compute(n + 1);

        Map<Integer, Multiset<String>> featureVector = h.featureVectorsByIteration();
        System.out.println(h.stabilizationIteration());
        System.out.println(featureVector.get(h.stabilizationIteration()));
        System.out.println(h.overallFeatureVector());
    }

    @Test
    public void test_WL_for_two_random_placeGraphs() {
        int n = 10;
        PureBigraph bigraph = provider.getRandomBigraphSingleRoot(n, 0);
        toPNG(bigraph, "wl-pg-big-1", DUMP_PATH);
        PureBigraph bigraph2 = provider.getRandomBigraphSingleRoot(n, 0);
        toPNG(bigraph2, "wl-pg-big-2", DUMP_PATH);

        PlaceGraphWLColorRefinementImpl wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraph);
        AbstractWLColorRefinementProcedure.WLResult hA = wlPlaceGraph.compute(n + 1);
        Map<Integer, Multiset<String>> featureVectorA = hA.featureVectorsByIteration();
        wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraph2);
        AbstractWLColorRefinementProcedure.WLResult hB = wlPlaceGraph.compute(n + 1);
        Map<Integer, Multiset<String>> featureVectorB = hB.featureVectorsByIteration();

        System.out.println("---SIZE---");
        System.out.println("bigraph = " + bigraph.getAllPlaces().size());
        System.out.println("bigraph2 = " + bigraph2.getAllPlaces().size());
        System.out.println("---SIZE---\r\n");

        // For iso check both graphs must fill the hashToInteger lookup table
        // otherwise they are detect as similar (because of the hierarchy but not because of 1-1 matching of control-labels
        // symmetric cases will be detected as similar as well
        Multiset<String> stringA = featureVectorA.get(hA.stabilizationIteration());
        Multiset<String> stringB = featureVectorB.get(hB.stabilizationIteration());
        System.out.println("Final Feature Vectors");
        System.out.println(stringA);
        System.out.println(stringB);

        System.out.println("\r\nOverall Feature Vectors");
        System.out.println(hA.overallFeatureVector());
        System.out.println(hB.overallFeatureVector());
    }

    @Test
    public void test_two_symmetric_similar_placeGraphs() throws Exception {
        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig);
        b.root()
//                .child("B").down().child("A").child("A").top() // when provided this from the other graph, they are not tested similar
                .child("A").down().child("B").child("B").top()
                .child("A").down().child("B").child("B").top()
        ;
        PureBigraph bigraphA = b.create();
        toPNG(bigraphA, "wl-pg-bigraph_symmetric_A", DUMP_PATH);
        PlaceGraphWLColorRefinementImpl wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraphA);
        AbstractWLColorRefinementProcedure.WLResult hA = wlPlaceGraph.compute(3);
        Map<Integer, Multiset<String>> featureVectorA = hA.featureVectorsByIteration();

        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(sig);
        b2.root()
//                .child("A").down().child("B").child("B").top() // when provided this from the other graph, they are not tested similar
                .child("B").down().child("A").child("A").top()
                .child("B").down().child("A").child("A").top()
        ;
        PureBigraph bigraphB = b2.create();
        toPNG(bigraphB, "wl-pg-bigraph_symmetric_B", DUMP_PATH);
        wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraphB);
        AbstractWLColorRefinementProcedure.WLResult hB = wlPlaceGraph.compute(3);

        // For iso check both graphs must fill the hashToInteger lookup table
        // otherwise they are detect as similar (because of the hierarchy but not because of 1-1 matching of control-labels
        // symmetric cases will be detected as similar as well
        // They are false detected similar
        Map<Integer, Multiset<String>> featureVectorB = hB.featureVectorsByIteration();
        Multiset<String> stringA = featureVectorA.get(hA.stabilizationIteration());
        Multiset<String> stringB = featureVectorB.get(hB.stabilizationIteration());
        System.out.println(stringA);
        System.out.println(stringB);
    }

    @Test
    public void test_WL_same_two_placeGraphs() throws Exception {
        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig);
        BigraphEntity.InnerName tmp = b.createInner("tmp");
        b.root()
                .child("A")
                .child("B").down().child("C").linkInner(tmp).top();
        b.root().child("A").child("B").down().child("D").linkInner(tmp).top();
        b.closeInner(tmp);
        PureBigraph bigraphA = b.create();
        toPNG(bigraphA, "wl-pg-bigraphA", DUMP_PATH);
        PlaceGraphWLColorRefinementImpl wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraphA);
        AbstractWLColorRefinementProcedure.WLResult hA = wlPlaceGraph.compute(20);
        Map<Integer, Multiset<String>> featureVectorA = hA.featureVectorsByIteration();

        // same bigraph as above, but only roots swapped (technically)
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(sig);
        BigraphEntity.InnerName tmp2 = b2.createInner("tmp");
        b2.root() // here we swap the two nodes under the first root
                .child("B").down().child("C").linkInner(tmp).top()
                .child("A").top();
        b2.root().child("A").child("B").down().child("D").linkInner(tmp).top();
        b2.closeInner(tmp2);
        PureBigraph bigraphB = b2.create();
        toPNG(bigraphB, "wl-pg-bigraphB", DUMP_PATH);
        wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraphB);
        AbstractWLColorRefinementProcedure.WLResult hB = wlPlaceGraph.compute(20); // coloring will be still the same

        Map<Integer, Multiset<String>> featureVectorB = hB.featureVectorsByIteration();
        Multiset<String> stringA = featureVectorA.get(hA.stabilizationIteration());
        Multiset<String> stringB = featureVectorB.get(hB.stabilizationIteration());
        System.out.println(stringA);
        System.out.println(stringB);
    }

    @Test
    public void test_WL_different_two_placeGraphs() throws Exception {
        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig);
        BigraphEntity.InnerName tmp = b.createInner("tmp");
        b.root()
                .child("A")
                .child("B").down().child("C").linkInner(tmp).top();
        b.root().child("A").child("B").down().child("D").linkInner(tmp).top();
        b.closeInner(tmp);
        PureBigraph bigraphA = b.create();
        toPNG(bigraphA, "wl-pg-bigraphA", DUMP_PATH);


        // same bigraph as above, but only roots swapped (technically)
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(sig);
        BigraphEntity.InnerName tmp2 = b2.createInner("tmp");
        b2.root()
                .child("H").child("H")
//                .child("I").down().child("J").linkInner(tmp).top()
                .child("I").linkInner(tmp).top()
                .child("F").child("G").down().child("K").child("J").linkInner(tmp).top();
        b2.closeInner(tmp2);
        PureBigraph bigraphB = b2.create();
        toPNG(bigraphB, "wl-pg-bigraphB", DUMP_PATH);
        System.out.println("---SIZE---");
        System.out.println(bigraphA.getAllPlaces().size());
        System.out.println(bigraphB.getAllPlaces().size());
        System.out.println("---SIZE---\r\n");

        PlaceGraphWLColorRefinementImpl wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraphA);
        AbstractWLColorRefinementProcedure.WLResult h = wlPlaceGraph.compute(15);
        System.out.println("Feature Vector Bigraph A = \r\n" + h.colorsByIteration().entrySetAt(h.stabilizationIteration()));

        wlPlaceGraph = new PlaceGraphWLColorRefinementImpl(bigraphB);
        h = wlPlaceGraph.compute(15);
        System.out.println("Feature Vector Bigraph B = \r\n" + h.colorsByIteration().entrySetAt(h.stabilizationIteration()));
    }
}
