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

import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.wl.WeisfeilerLehmanGraphKernel;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FrobeniusNormOnGraphKernelTests implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/wl-kernel-frobenius-norm/";

    DemoBigraphProvider provider = new DemoBigraphProvider();

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("java.awt.headless", "false");
        File dump = new File(DUMP_PATH);
        dump.mkdirs();
        FileUtils.cleanDirectory(new File(DUMP_PATH));
    }


    /**
     * Performs an iterative random search for bigraphs that increasingly match a target bigraph
     * according to the Frobenius norm of the Weisfeiler–Lehman graph-kernel matrix.
     * <p>
     * In each iteration, a random bigraph is generated and compared with the target using the
     * Weisfeiler–Lehman graph kernel. The Frobenius norm of the resulting kernel matrix is used
     * to evaluate the candidate. A candidate is retained whenever it improves upon the current
     * value. The search terminates when the target's self-kernel value is reached or when the
     * maximum number of iterations is exhausted.
     */
    @Test
    void test_graphKernelDistance_randomBigraphsConvergence() throws Exception {
        DynamicSignature sig = provider.createAlphabetSignature();
        int n = 4; // 1 root, n-1 nodes, 0 sites
        PureBigraph targetGraph = provider.getRandomBigraphSingleRoot(n, 0, sig);
        toPNG(targetGraph, "wl-big-random-target", DUMP_PATH);
        WeisfeilerLehmanGraphKernel kernelTarget = new WeisfeilerLehmanGraphKernel(targetGraph, targetGraph);
        double selfDist = NormOps_DDRM.inducedP2(kernelTarget.computeGraphKernel(n).getKernelMatrix().getLeft().getDDRM());
        System.out.println("Self-Distance: " + selfDist);

        DynamicSignature reducedSig = buildSignatureFromNodes(targetGraph);
        int maxIterations = 50000;
        System.out.println();
        double distance = 0;
        double sumPercentageIncrease = 0;
        for (int i = 0; i < maxIterations; i++) {
            PureBigraph bigraph2 = provider.getRandomBigraphSingleRoot(n, 0, reducedSig);
            WeisfeilerLehmanGraphKernel kernel = new WeisfeilerLehmanGraphKernel(bigraph2, targetGraph);
            WeisfeilerLehmanGraphKernel.GraphKernelResult graphKernelResult = kernel.computeGraphKernel(n);
            SimpleMatrix diracKernelResult_PG = graphKernelResult.getKernelMatrix().getLeft();
            // Computes the Frobenius norm of the WL graph-kernel matrix.
            double v = NormOps_DDRM.inducedP2(diracKernelResult_PG.getDDRM());

            // Retain the candidate if it improves the current value.
            if (distance < v) {
                double percentageIncrease = (1 - (distance / v));
                toPNG(bigraph2, "wl-big-random-" + i, DUMP_PATH);
                System.out.println("Iteration i = " + i);
                System.out.println("Improvement = " + percentageIncrease + " % (" + (v) + ")");
                sumPercentageIncrease += percentageIncrease;

                distance = v;
                if (distance >= selfDist) {
                    System.out.println("Optimum found.");
                    break;
                }
            }
        }

        System.out.println("Cumulative percentage improvement: " + sumPercentageIncrease); // je höher, desto mehr lokale Extrema
        System.out.println("Normalized cumulative improvement: " + sumPercentageIncrease / selfDist);
        System.out.println("Inverse normalized improvement: " + selfDist / sumPercentageIncrease);
    }

    public DynamicSignature buildSignatureFromNodes(PureBigraph bigraph) {
        DynamicSignatureBuilder sb = pureSignatureBuilder();
        bigraph.getNodes().forEach(x -> {
            sb.add(x.getControl());
        });
        return sb.create();
    }
}
