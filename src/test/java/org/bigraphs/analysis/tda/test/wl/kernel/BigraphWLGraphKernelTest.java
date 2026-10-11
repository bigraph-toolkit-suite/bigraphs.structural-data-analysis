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
package org.bigraphs.analysis.tda.test.wl.kernel;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;

import javax.swing.*;
import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.test.wl.kernel.gui.MatrixColorGUI;
import org.bigraphs.analysis.tda.wl.WeisfeilerLehmanGraphKernel;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BigraphWLGraphKernelTest implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/wl-graphkernel-test/";

    DemoBigraphProvider provider = new DemoBigraphProvider();

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
    }

    @Test
    @Disabled
    void graphKernel_GUI_Presentation_for_twoSameBigraphs() throws Exception {
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

        WeisfeilerLehmanGraphKernel kernel = new WeisfeilerLehmanGraphKernel(bigraphA, bigraphB);
        WeisfeilerLehmanGraphKernel.GraphKernelResult graphKernelResult = kernel.computeGraphKernel(4);

        // for place graph
        SimpleMatrix diracKernelResult_PG = graphKernelResult.getKernelMatrix().getLeft();
        SimpleMatrix diracKernelResult_LG = graphKernelResult.getKernelMatrix().getRight();

        System.out.println("Place Graph = \r\n" + diracKernelResult_PG);
        System.out.println("Link Graph = \r\n" + diracKernelResult_LG);

        String[] columnNames = graphKernelResult.getColumnLabelsForPG();
        String[] columnNames2 = graphKernelResult.getColumnLabelsForLG();
        double[][] data = diracKernelResult_PG.toArray2();
        double[][] data2 = diracKernelResult_LG.toArray2();

        SwingUtilities.invokeLater(() -> new MatrixColorGUI(data, columnNames));
        SwingUtilities.invokeLater(() -> new MatrixColorGUI(data2, columnNames2));

        while (true) {
            Thread.sleep(100);
        }
    }

    @Test
    @Disabled
    void graphKernel_GUI_Presentation_for_twoRandomBigraphs() throws Exception {
        int n = 5;
        PureBigraph bigraph1 = provider.getRandomBigraphSingleRoot(n, 1);
        toPNG(bigraph1, "wl-kernel-random-1", DUMP_PATH);

        PureBigraph bigraph2 = provider.getRandomBigraphSingleRoot(n, 1);
        toPNG(bigraph2, "wl-kernel-random-2", DUMP_PATH);

        WeisfeilerLehmanGraphKernel kernel = new WeisfeilerLehmanGraphKernel(bigraph1, bigraph2);
        WeisfeilerLehmanGraphKernel.GraphKernelResult graphKernelResult = kernel.computeGraphKernel(n + 1);

        SimpleMatrix diracKernelResult_PG = graphKernelResult.getKernelMatrix().getLeft();
        SimpleMatrix diracKernelResult_LG = graphKernelResult.getKernelMatrix().getRight();
        String[] columnNames = graphKernelResult.getColumnLabelsForPG();
        String[] columnNames2 = graphKernelResult.getColumnLabelsForLG();

        SwingUtilities.invokeLater(() -> new MatrixColorGUI(diracKernelResult_PG.toArray2(), columnNames, "Place Graph Matrix"));
        SwingUtilities.invokeLater(() -> new MatrixColorGUI(diracKernelResult_LG.toArray2(), columnNames2, "Link Graph Matrix"));

        while (true) {
            Thread.sleep(100);
        }
    }
}
