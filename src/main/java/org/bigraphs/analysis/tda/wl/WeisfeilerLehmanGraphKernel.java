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
package org.bigraphs.analysis.tda.wl;

import static org.bigraphs.framework.core.utils.BigraphUtil.getUniqueLabelOfBigraphEntity;

import com.google.common.collect.Multiset;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.bigraphs.analysis.tda.wl.impl.LinkGraphWLColorRefinementImpl;
import org.bigraphs.analysis.tda.wl.impl.PlaceGraphWLColorRefinementImpl;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.utils.BigraphUtil;
import org.ejml.simple.SimpleMatrix;

/**
 * Implements the Weisfeiler–Lehman (WL) subtree graph kernel, a neighborhood-aggregation kernel adapted
 * to bigraphs with discrete labels.
 * <p>
 * The kernel is based on the 1-WL color-refinement procedure implemented in {@link AbstractWLColorRefinementProcedure}.
 * <p>
 * At each refinement iteration, the kernel constructs a histogram of node labels. The Dirac kernel
 * is used to compare discrete labels. The histograms from the initial labeling and subsequent refinement
 * iterations are combined into feature vectors, and the similarity between two bigraphs is computed
 * as the inner product of their feature vectors.
 *
 * @author Dominik Grzelak
 * @see <a href="https://www.jmlr.org/papers/v12/shervashidze11a.html">Shervashidze et al., "Weisfeiler-Lehman Graph Kernels" (2011)</a>
 * @see <a href="https://doi.org/10.1007/s41109-019-0195-3">Kriege, Johansson, and Morris, "A Survey on Graph Kernels" (2020)</a>
 * @see <a href="https://doi.org/10.1109/SFCS.1979.8">Babai and Kučera, "Canonical Labelling of Graphs in Linear Average Time" (1979)</a>
 * @see <a href="https://proceedings.mlr.press/v97/rieck19a.html">Rieck, Bock, and Borgwardt, "A Persistent Weisfeiler-Lehman Procedure for Graph Classification" (2019)</a>
 */
public class WeisfeilerLehmanGraphKernel {

    PlaceGraphWLColorRefinementImpl wlPlaceGraphA, wlPlaceGraphB;
    LinkGraphWLColorRefinementImpl wlLinkGraphA, wlLinkGraphB;

    PureBigraph bigraphA;
    PureBigraph bigraphB;

    public record WLPairResult(
            AbstractWLColorRefinementProcedure.WLResult left,
            AbstractWLColorRefinementProcedure.WLResult right
    ) {
    }

    WLPairResult placeResults;
    WLPairResult linkResults;

    protected final Map<String, String> hashToIntegerLabel = new LinkedHashMap<>();
    int runningInteger = 1;

    public WeisfeilerLehmanGraphKernel(PureBigraph bigraphA, PureBigraph bigraphB) {
        this.bigraphA = bigraphA;
        this.bigraphB = bigraphB;
        this.wlPlaceGraphA = new PlaceGraphWLColorRefinementImpl(bigraphA);
        this.wlPlaceGraphB = new PlaceGraphWLColorRefinementImpl(bigraphB);
        this.wlLinkGraphA = new LinkGraphWLColorRefinementImpl(bigraphA);
        this.wlLinkGraphB = new LinkGraphWLColorRefinementImpl(bigraphB);
    }

    protected void computeNodeColoring(int maxIteration) {
        // Place graph
        placeResults = this.wlRelabelingFor(maxIteration, wlPlaceGraphA, wlPlaceGraphB);
        // Link graph
        linkResults = this.wlRelabelingFor(maxIteration, wlLinkGraphA, wlLinkGraphB);
    }

    /**
     * The sum of all 1-WL bigraph kernels over all iterations h the bigraph WL graph kernel.
     * The inner product of both feature vectors (of the two bigraphs) at iteration h is taken using the dirac kernel
     * as the base kernel for comparison.
     * Then, The sum of all kernel matrices is taken for h = 0...n
     *
     * @param maxIteration iteration count, higher values means more stable color labels
     * @return tuple of the WL graph kernel using the dirac kernel for the place and link graph
     */
    public GraphKernelResult computeGraphKernel(int maxIteration) {
        Pair<SimpleMatrix, SimpleMatrix> diracTuple;

        computeNodeColoring(maxIteration);

        // Place Graph
        SimpleMatrix kernelMatrixSumPG = diracKernelSumUntilFor(maxIteration, wlPlaceGraphA, wlPlaceGraphB);

        // Link Graph
        SimpleMatrix kernelMatrixSumLG = diracKernelSumUntilFor(maxIteration, wlLinkGraphA, wlLinkGraphB);

        // Return the tuple
        GraphKernelResult graphKernelResult = GraphKernelResult.create();
        diracTuple = Pair.of(kernelMatrixSumPG, kernelMatrixSumLG);
        graphKernelResult.withKernelResult(diracTuple);

        // Set row and column labels for the kernel matrix
        initRowAndColLabels(maxIteration, wlPlaceGraphA, wlPlaceGraphB, graphKernelResult);
        initRowAndColLabels(maxIteration, wlLinkGraphA, wlLinkGraphB, graphKernelResult);
        return graphKernelResult;
    }

    public void initRowAndColLabels(int maxIteration, AbstractWLColorRefinementProcedure wlA,
                                    AbstractWLColorRefinementProcedure wlB, GraphKernelResult kernelResultObj) {
        if (wlA instanceof PlaceGraphWLColorRefinementImpl && wlB instanceof PlaceGraphWLColorRefinementImpl) {
            kernelResultObj.withRowLabelsPG(wlA.L.keySetAt(maxIteration - 1).stream().map(BigraphUtil::getUniqueLabelOfBigraphEntity).toArray(String[]::new));
            kernelResultObj.withColumnLabelsPG(wlB.L.keySetAt(maxIteration - 1).stream().map(BigraphUtil::getUniqueLabelOfBigraphEntity).toArray(String[]::new));
        }
        if (wlA instanceof LinkGraphWLColorRefinementImpl && wlB instanceof LinkGraphWLColorRefinementImpl) {
            kernelResultObj.withRowLabelsLG(wlA.L.keySetAt(maxIteration - 1).stream().map(BigraphUtil::getUniqueLabelOfBigraphEntity).toArray(String[]::new));
            kernelResultObj.withColumnLabelsLG(wlB.L.keySetAt(maxIteration - 1).stream().map(BigraphUtil::getUniqueLabelOfBigraphEntity).toArray(String[]::new));
        }
    }

    /**
     * This method Computes the Weisfeiler–Lehman kernel matrix by summing the Dirac kernel matrices
     * obtained from the two WL feature vectors over all iterations.
     * <p>
     * For each iteration {@code h = 0, ..., maxIteration - 1}, a Dirac kernel matrix is
     * computed from the corresponding feature vectors of the two WL color-refinement
     * procedures. The resulting matrices are then summed element-wise.
     * <p>
     * Both arguments must represent the same graph subtype: either place graphs or link
     * graphs. Place-graph and link-graph refinement procedures cannot be mixed.
     *
     * @param maxIteration the number of WL refinement iterations to include
     * @param wlA          the WL color-refinement procedure for the first place or link graph
     * @param wlB          the WL color-refinement procedure for the second place or link graph
     * @return the sum of the Dirac kernel matrices over all WL refinement iterations
     */
    public SimpleMatrix diracKernelSumUntilFor(int maxIteration,
                                               AbstractWLColorRefinementProcedure wlA,
                                               AbstractWLColorRefinementProcedure wlB) {
        int rowSize = wlA.getFeatureVector().get(maxIteration - 1).size();
        int colSize = wlB.getFeatureVector().get(maxIteration - 1).size();
        if (rowSize == 0) {
            return new SimpleMatrix(new double[1][colSize]);
        }
        Map<Integer, SimpleMatrix> diracKernelMatrixTable = new LinkedHashMap<>();

        for (int n = 0; n < maxIteration; n++) {
            SimpleMatrix matrix0 = diracKernelFor(n, wlA, wlB); //k_diracKernel_PG(n, rows.size(), cols.size()); // dirac kernel matrix
            diracKernelMatrixTable.put(n, matrix0);
        }
        SimpleMatrix kernelMatrixSum = new SimpleMatrix(new double[rowSize][colSize]);
        for (int n = 0; n < maxIteration; n++) {
            SimpleMatrix kdiracMatrix = diracKernelMatrixTable.get(n);
            kernelMatrixSum = kernelMatrixSum.plus(kdiracMatrix);
        }
        return kernelMatrixSum;
    }

    public SimpleMatrix diracKernelFor(int iterationH,
                                       AbstractWLColorRefinementProcedure wlA,
                                       AbstractWLColorRefinementProcedure wlB) {
        Multiset<String> featureVectorFirst = wlA.getFeatureVector().get(iterationH);
        Multiset<String> featureVectorSecond = wlB.getFeatureVector().get(iterationH);
        SimpleMatrix diracKernelMatrix = new SimpleMatrix(new double[featureVectorFirst.size()][featureVectorSecond.size()]);
        int vi = 0, wi = 0;
        for (String v : featureVectorFirst) {
            wi = 0;
            for (String w : featureVectorSecond) {
                double newVal = v.equals(w) ? 1 : 0;
                diracKernelMatrix.set(vi, wi, newVal);
                wi++;
            }
            vi++;
        }
        return diracKernelMatrix;
    }

    /**
     * Do the relabeling either for the place graph or the link graph.
     * Therefore, just provide the respective instances as arguments.
     * Both must be of the same subtype, either place graphs, or link graphs.
     * They cannot be mixed.
     * <p>
     * Basically, this procedure resembles the one implemented in {@link BWLIsomorphismChecker}.
     *
     * @param maxIteration number of maximum iterations for the 1-WL algo
     * @param wlA          WL color labeling instance (place or link graph)
     * @param wlB          WL color labeling instance (place or link graph)
     */
    public WLPairResult wlRelabelingFor(int maxIteration,
                                        AbstractWLColorRefinementProcedure wlA,
                                        AbstractWLColorRefinementProcedure wlB) {
        hashToIntegerLabel.clear();

        List<String> initialLabels = new ArrayList<>();
        wlA.getAllRelevantBigraphEntities().forEach(n -> {
            String simpleLabel = getUniqueLabelOfBigraphEntity(n);
            initialLabels.add(simpleLabel);
        });
        wlB.getAllRelevantBigraphEntities().forEach(n -> {
            String simpleLabel = getUniqueLabelOfBigraphEntity(n);
            initialLabels.add(simpleLabel);
        });

        hashesToIntegerLabel(initialLabels);
        wlA.init(this.hashToIntegerLabel);
        wlB.init(this.hashToIntegerLabel);
        assert wlA.getHashToIntegerLabel().equals(wlB.getHashToIntegerLabel());

        int stabilizedAtA = -1;
        int stabilizedAtB = -1;
        boolean stabilizedA = false;
        boolean stabilizedB = false;

        for (int h = 1; h < maxIteration; h++) {
            final int hOld = h - 1;

            Map<BigraphEntity, String> bigraphEntityToHashA = wlA.step_2and3(hOld, h);
            Map<BigraphEntity, String> bigraphEntityToHashB = wlB.step_2and3(hOld, h);

            LinkedList<String> allGeneratedHashes = new LinkedList<>(bigraphEntityToHashA.values());
            allGeneratedHashes.addAll(bigraphEntityToHashB.values());

            hashesToIntegerLabel(allGeneratedHashes);

            wlA.updateLookupTable(this.hashToIntegerLabel);
            wlB.updateLookupTable(this.hashToIntegerLabel);

            assert wlA.getHashToIntegerLabel().equals(wlB.getHashToIntegerLabel());

            wlA.step4(bigraphEntityToHashA, h);
            wlB.step4(bigraphEntityToHashB, h);

            Multiset<String> currentColorsA = wlA.getFeatureVector().get(h);
            Multiset<String> currentColorsB = wlB.getFeatureVector().get(h);

            int resultA = wlA.finalStep(currentColorsA, h);
            int resultB = wlB.finalStep(currentColorsB, h);

            if (!stabilizedA && resultA != -1) {
                stabilizedAtA = resultA;
                stabilizedA = true;
            }
            if (!stabilizedB && resultB != -1) {
                stabilizedAtB = resultB;
                stabilizedB = true;
            }
        }

        return new WLPairResult(
                wlA.buildResult(stabilizedAtA, stabilizedA),
                wlB.buildResult(stabilizedAtB, stabilizedB)
        );
    }

    /**
     * get the h-th feature vector pair (for the place and link graph) of the WL relabeling for the "left/first" bigraph.
     *
     * @param h iteration of the WL algo
     * @return pair of feature vectors for the place and link graph
     */
    public Pair<Multiset<String>, Multiset<String>> getFeatureVectorLeftAt(int h) {
        return Pair.of(wlPlaceGraphA.getFeatureVector().get(h), wlLinkGraphA.getFeatureVector().get(h));
    }

    /**
     * get the h-th feature vector pair (for the place and link graph) of the WL relabeling for the "right/second" bigraph.
     *
     * @param h iteration of the WL algo
     * @return pair of feature vectors for the place and link graph
     */
    public Pair<Multiset<String>, Multiset<String>> getFeatureVectorRightAt(int h) {
        return Pair.of(wlPlaceGraphB.getFeatureVector().get(h), wlLinkGraphB.getFeatureVector().get(h));
    }

    /**
     * Get the feature vector pair (for the place and link graph) for the "left/first" bigraph for all h iterations
     *
     * @return
     */
    public Pair<Map<Integer, Multiset<String>>, Map<Integer, Multiset<String>>> getFeatureVectorLeft() {
        return Pair.of(wlPlaceGraphA.getFeatureVector(), wlLinkGraphA.getFeatureVector());
    }

    /**
     * Get the feature vector pair (for the place and link graph) for the "right/second" bigraph for all h iterations
     *
     * @return
     */
    public Pair<Map<Integer, Multiset<String>>, Map<Integer, Multiset<String>>> getFeatureVectorRight() {
        return Pair.of(wlPlaceGraphB.getFeatureVector(), wlLinkGraphB.getFeatureVector());
    }

    public void hashesToIntegerLabel(List<String> inputList) {
        Set<String> collect = new TreeSet<>(Comparator
                .comparing(String::length) // First, compare by length
                .thenComparing(Comparator.naturalOrder()));
        collect.addAll(inputList);
        for (String hash : collect) {
            if (!hashToIntegerLabel.containsKey(hash)) {
                hashToIntegerLabel.put(hash, String.valueOf(runningInteger));
                runningInteger++;
            }
        }
    }

    /**
     * Returns the current state of the hashing function.
     * Nodes and their neighbors are assigned unique labels after each iteration.
     */
    public Map<String, String> getNodeHashes() {
        return new LinkedHashMap<>(hashToIntegerLabel);
    }

    /**
     * The result of a graph kernel computation.
     * It contains the kernel of the place and link graph of two bigraphs,
     * and row and column labels.
     */
    public static class GraphKernelResult {

        public static GraphKernelResult create() {
            return new GraphKernelResult();
        }

        Pair<SimpleMatrix, SimpleMatrix> kernelResultMatrix;

        String[] columnLabelsPG;
        String[] rowLabelsPG;

        String[] columnLabelsLG;
        String[] rowLabelsLG;

        public GraphKernelResult withColumnLabelsPG(String[] columnLabelsPG) {
            this.columnLabelsPG = columnLabelsPG;
            return this;
        }

        public GraphKernelResult withRowLabelsPG(String[] rowLabelsPG) {
            this.rowLabelsPG = rowLabelsPG;
            return this;
        }

        public GraphKernelResult withColumnLabelsLG(String[] columnLabelsLG) {
            this.columnLabelsLG = columnLabelsLG;
            return this;
        }

        public GraphKernelResult withRowLabelsLG(String[] rowLabelsLG) {
            this.rowLabelsLG = rowLabelsLG;
            return this;
        }

        public GraphKernelResult withKernelResult(Pair<SimpleMatrix, SimpleMatrix> diracTuple) {
            this.kernelResultMatrix = diracTuple;
            return this;
        }

        public String[] getColumnLabelsForPG() {
            return columnLabelsPG;
        }

        public String[] getColumnLabelsForLG() {
            return columnLabelsLG;
        }

        public String[] getRowLabelsForPG() {
            return rowLabelsPG;
        }

        public String[] getRowLabelsForLG() {
            return rowLabelsLG;
        }

        /**
         * Get the kernel matrix pair for the place and link graph (the result of the kernel computation)
         *
         * @return
         */
        public Pair<SimpleMatrix, SimpleMatrix> getKernelMatrix() {
            return kernelResultMatrix;
        }
    }
}
