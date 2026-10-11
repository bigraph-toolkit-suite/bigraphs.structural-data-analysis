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

import static org.bigraphs.framework.core.factory.BigraphFactory.ops;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.bigraphs.analysis.tda.wl.AbstractWLColorRefinementProcedure;
import org.bigraphs.analysis.tda.wl.impl.BiGridColorRefinementImpl;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.visualization.SwingGraphStreamer;
import org.bigraphs.model.provider.spatial.bigrid.BiGridElementFactory;
import org.bigraphs.model.provider.spatial.bigrid.ConvexShapeBuilder;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dominik Grzelak
 */
public class BiGrid1DWLColorLabelingTest implements BigraphUnitTestSupport {
    static final String DUMP_PATH = "src/test/resources/dump/wl-bigrid/";

    @Test
    @Disabled
    public void create_elem_bigrid() throws Exception {
        BiGridElementFactory factory = BiGridElementFactory.create();

        float stepSize = 0.1f;
        PureBigraph bigrid0 = factory.crossingFour(0, 0 * stepSize, stepSize);
        PureBigraph bigrid1 = factory.crossingFour(0, 1 * stepSize, stepSize);
        PureBigraph bigrid2 = factory.crossingFour(0, 2 * stepSize, stepSize);
        PureBigraph bigrid3 = factory.crossingFour(0, 3 * stepSize, stepSize);
        PureBigraph bigrid4 = factory.crossingFour(0, 4 * stepSize, stepSize);
        PureBigraph bigrid5 = factory.crossingFour(0, 5 * stepSize, stepSize);
        PureBigraph bigrid6 = factory.crossingFour(0, 6 * stepSize, stepSize);

        PureBigraph bigrid =
                ops(bigrid0)
                        // .merge(bigrid0)
                        // .merge(bigrid1)
                        // .merge(bigrid2)
                        // .merge(bigrid3)
                        // .merge(bigrid4)
                        // .merge(bigrid5)
                        // .merge(bigrid6)
                        .getOuterBigraph();

        BiGridColorRefinementImpl bigridEmb = new BiGridColorRefinementImpl(bigrid);
        AbstractWLColorRefinementProcedure.WLResult h = bigridEmb.compute(bigrid.getSupport().size() * 2);
        System.out.println("Feature Vector Size = " + h.overallFeatureVector().size());
        System.out.println("Feature Vector = \r\n" + Arrays.toString(h.overallFeatureVectorAsArray()));

        SwingGraphStreamer graphStreamer = new SwingGraphStreamer(bigrid)
                .renderSites(false)
                .renderRoots(false);
        graphStreamer.prepareSystemEnvironment();
        graphStreamer.getGraphViewer();
        while (true)
            Thread.sleep(10000);
    }

    @Test
    public void test_bigrid() throws InvalidConnectionException {
        float stepSize = 0.5f;
        float padding = 0.7f;
        List<Point2D.Float> convexPoints = new LinkedList<>();
        convexPoints.add(new Point2D.Float(0f, 0f));
        convexPoints.add(new Point2D.Float(0f, 1.5f));
        convexPoints.add(new Point2D.Float(4.5f, 0.15f));
        convexPoints.add(new Point2D.Float(1.70f, -2.65f));

        // Translation-invariant
        List<Point2D.Float> convexPoints2 = convexPoints.stream().map(p -> {
            return new Point2D.Float(p.x - 1, p.y + 2);
        }).toList();

        PureBigraph result = ConvexShapeBuilder.generateSingleRoot(convexPoints, stepSize, padding, BiGridElementFactory.create());
        PureBigraph result2 = ConvexShapeBuilder.generateSingleRoot(convexPoints2, stepSize, padding, BiGridElementFactory.create());
        toPNG(result, "left", DUMP_PATH);
        toPNG(result2, "right", DUMP_PATH);
        BiGridColorRefinementImpl wlLinkGraph = new BiGridColorRefinementImpl(result);
        AbstractWLColorRefinementProcedure.WLResult h = wlLinkGraph.compute(result.getSupport().size() * 2);
        System.out.println(h.overallFeatureVector().size());
        System.out.println(Arrays.toString(h.overallFeatureVectorAsArray()));


        BiGridColorRefinementImpl wlLinkGraph2 = new BiGridColorRefinementImpl(result2);
        AbstractWLColorRefinementProcedure.WLResult h2 = wlLinkGraph2.compute(result2.getSupport().size() * 2);
        System.out.println(h2.overallFeatureVector().size());
        System.out.println(Arrays.toString(h2.overallFeatureVectorAsArray()));

        float ratio = (float) h.overallFeatureVector().size() / h2.overallFeatureVector().size();
        System.out.println("Ratio: " + ratio);

    }
}
