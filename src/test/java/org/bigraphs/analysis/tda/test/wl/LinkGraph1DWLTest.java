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

import com.google.common.collect.Multiset;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.wl.AbstractWLColorRefinementProcedure;
import org.bigraphs.analysis.tda.wl.impl.LinkGraphWLColorRefinementImpl;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.model.provider.spatial.bigrid.BiGridElementFactory;
import org.bigraphs.model.provider.spatial.bigrid.ConvexShapeBuilder;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.junit.jupiter.api.Test;

/**
 * @author Dominik Grzelak
 */
public class LinkGraph1DWLTest implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/wl-lg/";

    DemoBigraphProvider provider = new DemoBigraphProvider();

    @Test
    public void test_bigrid() throws InvalidConnectionException {
        float stepSize = 0.5f;
        float padding = 0.7f;
        List<Point2D.Float> convexPoints = new LinkedList<>();
        convexPoints.add(new Point2D.Float(0f, 0f));
        convexPoints.add(new Point2D.Float(0f, 1.5f));
        convexPoints.add(new Point2D.Float(4.5f, 0.15f));
        convexPoints.add(new Point2D.Float(1.70f, -2.65f));

        PureBigraph result = ConvexShapeBuilder.generateSingleRoot(convexPoints, stepSize, padding, BiGridElementFactory.create());
        PureBigraph result2 = ConvexShapeBuilder.generateSingleRoot(convexPoints, stepSize, padding, BiGridElementFactory.create());
        LinkGraphWLColorRefinementImpl wlLinkGraph = new LinkGraphWLColorRefinementImpl(result);
        AbstractWLColorRefinementProcedure.WLResult h = wlLinkGraph.compute(result.getSupport().size() * 2);
        Map<Integer, Multiset<String>> featureVector = h.featureVectorsByIteration();
        System.out.println(featureVector.get(h.stabilizationIteration()));

        LinkGraphWLColorRefinementImpl wlLinkGraph2 = new LinkGraphWLColorRefinementImpl(result2);
        AbstractWLColorRefinementProcedure.WLResult h2 = wlLinkGraph2.compute(result2.getSupport().size() * 2);
        System.out.println("---------------");
        Map<Integer, Multiset<String>> featureVector2 = h2.featureVectorsByIteration();
        System.out.println(featureVector2.get(h2.stabilizationIteration()));

        assert featureVector.get(h.stabilizationIteration()).elementSet().containsAll(featureVector2.get(h2.stabilizationIteration()).elementSet());
        assert featureVector.get(h.stabilizationIteration()).size() == featureVector2.get(h2.stabilizationIteration()).size();
    }

    @Test
    public void test_WL_for_random_linkGraph() {
        int n = 10;
        PureBigraph bigraph = provider.getRandomBigraphSingleRootOnlyOuternames(n, 0);
        toPNG(bigraph, "wl-lg-big-1", DUMP_PATH);
        LinkGraphWLColorRefinementImpl wlLinkGraph = new LinkGraphWLColorRefinementImpl(bigraph);
        AbstractWLColorRefinementProcedure.WLResult h = wlLinkGraph.compute(n + 1);
        System.out.println("---------------");
        Map<Integer, Multiset<String>> featureVector = h.featureVectorsByIteration();
        System.out.println(featureVector.get(h.stabilizationIteration()));
    }

    @Test
    public void test_WL_for_two_random_linkGraphs() {
        int n = 10;
        PureBigraph bigraph = provider.getRandomBigraphSingleRootOnlyOuternames(n, 0);
        toPNG(bigraph, "wl-lg-big-1", DUMP_PATH);
        PureBigraph bigraph2 = provider.getRandomBigraphSingleRootOnlyOuternames(n, 0);
        toPNG(bigraph2, "wl-lg-big-2", DUMP_PATH);

        LinkGraphWLColorRefinementImpl wlLinkGraph = new LinkGraphWLColorRefinementImpl(bigraph);
        AbstractWLColorRefinementProcedure.WLResult hA = wlLinkGraph.compute(n + 1);
        Map<Integer, Multiset<String>> featureVectorA = hA.featureVectorsByIteration();

        wlLinkGraph = new LinkGraphWLColorRefinementImpl(bigraph2);
        AbstractWLColorRefinementProcedure.WLResult hB = wlLinkGraph.compute(n + 1);
        Map<Integer, Multiset<String>> featureVectorB = hB.featureVectorsByIteration();
        System.out.println("---------------");

        // For iso check both graphs must fill the hashToInteger lookup table
        // otherwise they are detect as similar (because of the hierarchy but not because of 1-1 matching of control-labels
        // symmetric cases will be detected as similar as well
        Multiset<String> stringA = featureVectorA.get(hA.stabilizationIteration());
        Multiset<String> stringB = featureVectorB.get(hB.stabilizationIteration());
        System.out.println(stringA);
        System.out.println(stringB);
    }
}
