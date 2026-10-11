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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.wl.AbstractWLColorRefinementProcedure;
import org.bigraphs.analysis.tda.wl.WeisfeilerLehmanGraphKernel;
import org.bigraphs.analysis.tda.wl.impl.PlaceGraphWLColorRefinementImpl;
import org.bigraphs.framework.core.analysis.PureLinkGraphConnectedComponents;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.visualization.SwingGraphStreamer;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Contains tests for analysing distributions derived from Weisfeiler–Lehman
 * feature vectors of bigraphs, including experiments related to Kullback–Leibler divergence.
 *
 * @author Dominik Grzelak
 */
public class KullbackLeiblerDivergenceTest implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/kl-distance/";

    DemoBigraphProvider provider = new DemoBigraphProvider();

    @BeforeEach
    void setUp() throws IOException {
        File dump = new File(DUMP_PATH);
        dump.mkdirs();
        FileUtils.cleanDirectory(new File(DUMP_PATH));
    }


    /**
     * This test demonstrates how to fit a normal distribution to a discrete distribution represented by
     * values and their associated probabilities.
     * <p>
     * The mean and standard deviation are estimated from the discrete distribution and used
     * to parameterise a {@link NormalDistribution}. The resulting probability density is then
     * evaluated at selected points.
     */
    @Test
    void default_descrete_to_continuous() {
        // Assuming you have a list of integers and their probabilities
        int[] values = {1, 2, 3, 4};
        double[] probabilities = {0.1, 0.2, 0.4, 0.3};

        // Convert to continuous distribution (using Normal Distribution as an example)
        double mean = calculateMean(values, probabilities);
        double stdDev = calculateStandardDeviation(values, probabilities);

        // Create a Normal Distribution
        NormalDistribution continuousDistribution = new NormalDistribution(mean, stdDev);

        // Print probabilities for some points in the continuous range
        for (double x = -1.0; x <= 6.0; x += 1.0) {
            double probability = continuousDistribution.density(x);
            System.out.println("Probability at " + x + ": " + probability);
        }
    }

    @Test
    void kl_between_two_random_bigraphs_differentSignature() throws InterruptedException {
        DynamicSignature sig = provider.createAlphabetSignature();
        DynamicSignature sig2 = provider.createRandomSignature();
        int n = 11; // 1 root, n-1 nodes, 0 sites
        PureBigraph firstGraph = provider.getRandomBigraphSingleRoot(n, 0, sig);
        toPNG(firstGraph, "kl-random-1", DUMP_PATH);
        PureBigraph secondGraph = provider.getRandomBigraphSingleRoot(n, 0, sig2);
        toPNG(secondGraph, "kl-random-2", DUMP_PATH);

        PlaceGraphWLColorRefinementImpl pgRef1 = new PlaceGraphWLColorRefinementImpl(firstGraph);
        PlaceGraphWLColorRefinementImpl pgRef2 = new PlaceGraphWLColorRefinementImpl(secondGraph);

        AbstractWLColorRefinementProcedure.WLResult h1 = pgRef1.compute(n);
        AbstractWLColorRefinementProcedure.WLResult h2 = pgRef2.compute(n);

//        int h = 0;
        System.out.println("first graph");
        Multiset<String> occurrencesFirst = h1.featureVectorsByIteration().get(h1.stabilizationIteration()); //PG
        Map<Integer, Double> countProbabilityMap = calculateRelativeFrequency(occurrencesFirst);
        System.out.println(countProbabilityMap);

        System.out.println("second graph");
        Multiset<String> occurrencesSecond = h2.featureVectorsByIteration().get(h2.stabilizationIteration()); //PG
        Map<Integer, Double> countProbabilityMap2 = calculateRelativeFrequency(occurrencesSecond);
        System.out.println(countProbabilityMap2);
    }

    @Test
    @Disabled
    void kl_between_two_random_bigraphs_sameSignature() throws InterruptedException {
        int n = 100; // 1 root, n-1 nodes, 0 sites

        DynamicSignature sig = provider.createAlphabetSignature();
        PureBigraph firstGraph = provider.getRandomBigraphSingleRoot(n, 0, sig);
        toPNG(firstGraph, "kl-random-1", DUMP_PATH);
        PureBigraph secondGraph = provider.getRandomBigraphSingleRoot(n, 0, sig);
        toPNG(secondGraph, "kl-random-2", DUMP_PATH);

        PureLinkGraphConnectedComponents cc = new PureLinkGraphConnectedComponents();
        cc.decompose(firstGraph);
        cc.decompose(secondGraph);

        SwingGraphStreamer gs = new SwingGraphStreamer(firstGraph);
        gs.prepareSystemEnvironment();
        gs
                .renderSites(false)
                .renderRoots(false)
                .getGraphViewer();

        new SwingGraphStreamer(secondGraph)
                .renderSites(false)
                .renderRoots(false)
                .getGraphViewer();


        WeisfeilerLehmanGraphKernel graphKernelFunc = new WeisfeilerLehmanGraphKernel(firstGraph, secondGraph);
        WeisfeilerLehmanGraphKernel.GraphKernelResult kernelResult = graphKernelFunc.computeGraphKernel(n);
        Pair<Map<Integer, Multiset<String>>, Map<Integer, Multiset<String>>> fvLeft = graphKernelFunc.getFeatureVectorLeft();
        Pair<Map<Integer, Multiset<String>>, Map<Integer, Multiset<String>>> fvRight = graphKernelFunc.getFeatureVectorRight();
        // Place graph norm
        double selfDist = NormOps_DDRM.inducedP2(kernelResult.getKernelMatrix().getLeft().getDDRM());
        System.out.println("Self-Distance: " + selfDist);

        int h = 2;
        System.out.println("\r\nFirst graph: ");
        Multiset<String> occurrencesFirst = fvLeft.getLeft().get(h);//PG
        Map<Integer, Double> countProbabilityMap = calculateRelativeFrequency(occurrencesFirst);
        System.out.println(countProbabilityMap);

        System.out.println("\r\nSecond graph:");
        Multiset<String> occurrencesSecond = fvRight.getLeft().get(h);//PG
        Map<Integer, Double> countProbabilityMap2 = calculateRelativeFrequency(occurrencesSecond);
        System.out.println(countProbabilityMap2);

        System.out.println("\r\n---------------------\r\n");

        //Map<String, String> nodeHashes = graphKernelFunc.getNodeHashes();
        //System.out.println(nodeHashes);

        boolean running = true;
        while (running) {
            Thread.sleep(500);
        }
    }


    private static double calculateMean(int[] values, double[] probabilities) {
        double mean = 0.0;
        for (int i = 0; i < values.length; i++) {
            mean += values[i] * probabilities[i];
        }
        return mean;
    }

    private static double calculateStandardDeviation(int[] values, double[] probabilities) {
        double mean = calculateMean(values, probabilities);
        double variance = 0.0;
        for (int i = 0; i < values.length; i++) {
            variance += probabilities[i] * (values[i] - mean) * (values[i] - mean);
        }
        return Math.sqrt(variance);
    }

    private static Map<Integer, Double> calculateRelativeFrequency(Multiset<String> multiset) {
        return calculateFrequency(multiset, true);
    }

    private static Map<Integer, Double> calculateFrequency(Multiset<String> multiset, boolean relativeFreq) {
        int totalElements = multiset.size();
        // Use traditional for loop to iterate over the elements
        Map<Integer, Double> countProbabilityMap = new HashMap<>();
        for (String element : multiset.elementSet().stream().sorted().toList()) {
            int count = multiset.count(element);
            double frequency = (double) count;
            if (relativeFreq)
                frequency = (double) count / totalElements;
            countProbabilityMap.put(Integer.valueOf(element), frequency);
        }
        return countProbabilityMap;
    }
}
