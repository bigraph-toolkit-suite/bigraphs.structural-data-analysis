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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.bigraphs.analysis.tda.util.IndexedHashMap;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;

/**
 * Implements the one-dimensional Weisfeiler–Lehman (1-WL) color-refinement procedure for a single bigraph.
 * The 1-WL algorithm, also known as naive vertex refinement, color refinement or canonical labeling, is a heuristic for graph
 * isomorphism testing.
 * <p>
 * This abstract class provides a template for refining the "colors of entities" in a bigraph's place graph
 * or link graph. Concrete implementations specialize the procedure for each graph structure and provide
 * the corresponding entities, interface entities, and neighborhood relation:
 * <ul>
 *   <li>{@link org.bigraphs.analysis.tda.wl.impl.PlaceGraphWLColorRefinementImpl}</li>
 *   <li>{@link org.bigraphs.analysis.tda.wl.impl.LinkGraphWLColorRefinementImpl}</li>
 * </ul>
 *
 * <p>
 * <strong>Color-refinement procedure</strong>
 *
 * <p>
 * Given a place graph or link graph, the procedure initializes each relevant entity with a color derived
 * from its label. For nodes, the label is determined by the control map; interface entities receive their
 * corresponding structural labels. These initial colors form the coloring at iteration {@code h = 0}.
 *
 * <p>
 * At each subsequent iteration {@code h}, the algorithm computes a new color for each entity from its
 * previous color and the multiset of colors assigned to its neighbors in iteration {@code h - 1}.
 * Each resulting signature is mapped to a color through a lookup table, ensuring that identical
 * signatures receive the same color.
 *
 * <p>
 * Refinement continues until the coloring stabilizes or the maximum iteration limit is reached.
 * Stabilization is detected when the number of distinct colors remains unchanged between consecutive
 * iterations.
 *
 * <p>
 * <strong>Comparing bigraphs</strong>
 *
 * <p>
 * Use {@link BWLIsomorphismChecker} to compare two bigraphs using this procedure. The comparison requires
 * a shared color-label lookup table so that corresponding signatures receive consistent color labels.
 * Note that 1-WL is not a complete graph-isomorphism test: matching color-refinement results do not
 * guarantee that two bigraphs are isomorphic.
 *
 * @author Dominik Grzelak
 * @see <a href="https://www.jmlr.org/beta/papers/v12/shervashidze11a.html">Shervashidze et al., "Weisfeiler-Lehman Graph Kernels" (2011)</a>
 * @see <a href="https://doi.org/10.1007/s41109-019-0195-3">Kriege, Johansson, and Morris, "A Survey on Graph Kernels" (2020)</a>
 * @see <a href="https://proceedings.mlr.press/v97/rieck19a.html">Rieck, Bock, and Borgwardt, "A Persistent Weisfeiler-Lehman Procedure for Graph Classification" (2019)</a>
 */
public abstract class AbstractWLColorRefinementProcedure {

    public record WLResult(
            int stabilizationIteration,
            boolean stabilized,
            IndexedHashMap<BigraphEntity<?>, String> colorsByIteration,
            Map<Integer, Multiset<String>> featureVectorsByIteration,
            Map<String, String> labelDictionary
    ) {

        public int[] overallFeatureVectorAsArray() {
            return overallFeatureVector().stream().mapToInt(Integer::intValue).toArray();
        }

        public List<Integer> overallFeatureVector() {
            List<Integer> overall = new ArrayList<>();

            featureVectorsByIteration.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(iterEntry -> {
                        Multiset<String> iterationVector = iterEntry.getValue();

                        iterationVector.entrySet().stream()
                                .sorted(Comparator.comparing(Multiset.Entry::getElement))
                                .forEach(featureEntry -> overall.add(featureEntry.getCount()));
                    });

            return Collections.unmodifiableList(overall);
        }
    }

    private boolean debugEnabled = false;

    // Node color map: Bigraph entities to colors (compressed/hased labels)
    protected IndexedHashMap<BigraphEntity<?>, String> C = new IndexedHashMap<>(); // C_{h,n}, h=iteration, n=node, defined for h>=0

    // for a bigraph entity at iteration h: a tuple containing the compressed label of that bigraph entity,
    // and the compressed labels of all neighboring nodes.
    // the tuple has to be combined to one multiset to generate a new a compressed label (i.e., a new hash)
    protected IndexedHashMap<BigraphEntity<?>, Pair<String, Multiset<String>>> L = new IndexedHashMap<>(); // L_{h,n}, h=iteration, n=node, defined for h>0

    // the bigraph for the WL-PG procedure
    protected PureBigraph bigraph;

    protected HashFunction hashFunction;
    protected final Map<String, String> hashToIntegerLabel = new LinkedHashMap<>();
    int runningInteger = 1;
    protected final Multiset<String> previousColors = HashMultiset.create();
    Map<Integer, Multiset<String>> featureVector = new LinkedHashMap<>();

    public AbstractWLColorRefinementProcedure(PureBigraph bigraph) {
        assert bigraph != null;
        this.bigraph = bigraph;
    }

    // All relevant nodes for a place graph (roots, sites, nodes) or link graph (inner, outer, nodes)
    // The order is important and different implementations cannot be mixed when comparing two graphs. It belongs to the procedure.
    public abstract List<BigraphEntity<?>> getAllRelevantBigraphEntities();

    // interface nodes for a place graph (roots, sites) or link graph (inner, outer)
    // The order is important and different implementations cannot be mixed when comparing two graphs. It belongs to the procedure.
    public abstract List<BigraphEntity<?>> getInterfaceBigraphEntities();

    // Get all neighboring nodes of a node.
    // Open neighborhood function is specific to place graphs and link graphs.
    // The order is important and different implementations cannot be mixed when comparing two graphs. It belongs to the procedure.
    public abstract List<BigraphEntity<?>> getOpenNeighborHoodOf(BigraphEntity<?> bigraphEntity);

    // init with empty map: no initial hashed colouring is used
    protected void init() {
        this.init(Collections.emptyMap());
    }

    /**
     * provide initial hashing lookup table for compressed "neighbor" labels to simple integer labels.
     * Uses {@link #updateLookupTable(Map)} if provided map is not empty to update
     *
     * @param hashToIntegerLabel0
     */
    protected void init(Map<String, String> hashToIntegerLabel0) {
        List<String> initialLabels = new ArrayList<>();
        previousColors.clear();
        hashToIntegerLabel.clear();
        if (hashToIntegerLabel0 != null && !hashToIntegerLabel0.isEmpty()) {
            updateLookupTable(hashToIntegerLabel0);
        }
        featureVector.clear();
        // define any suitable hashing function to generate compressed labels from multisets
        // there is also another hashing function that is based on the ordering of the multiset containing neighboring nodes
        // these are the fastest according to the internal code documentation of `org.google.common.hash.Hashing`
//        hashFunction = Hashing.murmur3_128();
        hashFunction = Hashing.fingerprint2011();

        // (1) Step (1)
        // Create initial compressed labels: Initialize initial node coloring
        // by creating simple integer labels
        // supply all relevant bigraph entities
        getAllRelevantBigraphEntities()
                .forEach(n -> {
                    String simpleLabel = getUniqueLabelOfBigraphEntity(n);
                    initialLabels.add(simpleLabel);
                });
        // only do initial hashing/mapping if we start with an empty lookup table from compressed labels -> simple integer labels
        if (hashToIntegerLabel0 == null || hashToIntegerLabel0.isEmpty()) {
            hashesToIntegerLabel(initialLabels);
        }
        getAllRelevantBigraphEntities()
                .forEach(n -> {
                    String compressedSimpleLabel = getUniqueLabelOfBigraphEntity(n);
                    compressedSimpleLabel = hashToIntegerLabel.get(compressedSimpleLabel);
                    C.addAt(0, n, compressedSimpleLabel);
                    previousColors.add(compressedSimpleLabel); // for comparison later at each iteration
                });
        if (C.sizeAt(0) != -1) {
            assert C.sizeAt(0) == this.getAllRelevantBigraphEntities().size();
        }
        featureVector.put(0, HashMultiset.create(previousColors));
    }


    /**
     * Uses default max iteration of h = |nodes|+1
     *
     * @return
     */
    public WLResult compute() {
        return compute(getAllRelevantBigraphEntities().size() + 1);
    }

    // Main entrypoint of the WL procedure
    public WLResult compute(int maxIteration) {
        this.init();
        for (int h = 1; h < maxIteration; h++) {
            final int hOld = h - 1;
            int finalH = h;

            // (2-3) Step (2-3): Sorting each Multiset and Label compression
            Map<BigraphEntity, String> bigraphEntityToHash = step_2and3(hOld, finalH);

            // (4) Step (4): Relabeling
            // Now complete iteration h: set new compressed label for each n in C_{h,n}
            // Therefore, we translate the string hashes to simple integer labels
            // we get the bigraph nodes in the same order as before
            // Convert long string hashes to simple integer labels and augment the lookup table
            // This helps also to partition the nodes regardless of their label meaning
            // sort all string hashes together in ascending order
            LinkedList<String> allGeneratedHashes = new LinkedList<>(bigraphEntityToHash.values());
            hashesToIntegerLabel(allGeneratedHashes);// hashesToIntegerLabel(ALL_LABELS);
            step4(bigraphEntityToHash, finalH);

            // Final Step
            // Check if we are finished: colors are stabilized or not: It's the case only when no new colors are added
            // Return the current iteration counter or -1 to continue node coloring
            Multiset<String> currentColors = featureVector.get(finalH);
            int currentIteration = finalStep(currentColors, finalH);
            if (currentIteration != -1) {
                return buildResult(currentIteration, true);
            }
        }

        return buildResult(-1, false);
    }

    public WLResult buildResult(int stabilizationIteration, boolean stabilized) {
        return new WLResult(
                stabilizationIteration,
                stabilized,
                C,
                featureVector,
                Map.copyOf(hashToIntegerLabel)
        );
    }


    Map<BigraphEntity, String> step_2and3(int hOld, int finalH) {
        // Helper map for 'hashCodeToInteger'
        Map<BigraphEntity, String> bigraphEntityToHash = new LinkedHashMap<>();
        // Collect labels, iterate through bigraph nodes in the same order
        // (2) Step (2)
        getAllRelevantBigraphEntities()
                .forEach(node -> {
                    // node’s previous label
                    String oldLabelOfNode = C.getValueFromMapAt(hOld, node);
                    // multiset of previous compressed node labels from all nodes 'm' neighboring 'node'
                    Multiset<String> multisetNeighbors = HashMultiset.create();
                    getOpenNeighborHoodOf(node).forEach(m -> {
                        multisetNeighbors.add(C.getValueFromMapAt(hOld, m));
                    });
                    Pair<String, Multiset<String>> labelPair = Pair.of(oldLabelOfNode, multisetNeighbors);
                    L.addAt(finalH, node, labelPair);
                    // Compute hashes for each 'n' in L_{h,n}
                    // Create new set of colors that is mapped to a new color
                    // For graph isomorphism check, the WL algorithm requires canonical forms of the multisets.
                    // Therefore, we arrange the elements of the multiset in sorted order.
                    // Choose the hashing function here
                    String compressedLabel = hash(labelPair);
                    bigraphEntityToHash.put(node, compressedLabel);
                });

        return bigraphEntityToHash;
    }


    void step4(Map<BigraphEntity, String> bigraphEntityToHash, int finalH) {
        Multiset<String> currentColors = HashMultiset.create();
        Set<BigraphEntity<?>> bigraphEntities = L.keySetAt(finalH);
        for (BigraphEntity<?> each : bigraphEntities) {
            String simpleIntegerLabel = hashToIntegerLabel.get(bigraphEntityToHash.get(each));
            currentColors.add(simpleIntegerLabel);
            C.addAt(finalH, each, simpleIntegerLabel);
        }
        // add colors as new feature vector for iteration h
        featureVector.put(finalH, currentColors);
    }

    /**
     * Determines whether the WL color refinement has stabilised at the current iteration.
     * <p>
     * Stabilisation is reached when the number of distinct colors in the current coloring equals the number
     * of distinct colors in the previous coloring. If the coloring has not stabilised, the current coloring
     * becomes the reference for the next iteration.
     *
     * @param currentColors the multiset of colors generated in the current iteration
     * @param finalH        the current WL iteration
     * @return {@code finalH} if the coloring has stabilised; {@code -1} otherwise
     * @see <a href="https://doi.org/10.1007/s41109-019-0195-3">
     * Kriege, Johansson, and Morris, "A Survey on Graph Kernels" (2020)</a>
     */
    int finalStep(Multiset<String> currentColors, int finalH) {
        if (previousColors.elementSet().size() == (currentColors.elementSet().size())) {
            if (debugEnabled) {
                debugLog("Previous multiset: " + previousColors + " (" + previousColors.size() + ")");
                debugLog("Current multiset:  " + currentColors + " (" + currentColors.size() + ")");
                debugLog("Distinct previous labels: " +
                        previousColors.elementSet().stream()
                                .sorted(String::compareTo)
                                .toList() +
                        " (" + previousColors.elementSet().size() + ")");
                debugLog("Distinct current labels:  " +
                        currentColors.elementSet().stream()
                                .sorted(String::compareTo)
                                .toList() +
                        " (" + currentColors.elementSet().size() + ")");
            }
            return finalH;
        } else {
            previousColors.clear();
            previousColors.addAll(HashMultiset.create(currentColors));
            return -1;
        }
    }

    /**
     * Implements the relabelling function of the 1-WL kernel.
     * <p>
     * The relabelling function constructs a new label from the current node label and the multiset of labels
     * of its neighbours. For this, the neighbouring labels are supplied in sorted order so
     * that their concatenation is deterministic.
     * <p>
     * As described in the original paper, sorting the multiset itself is not required by the 1-WL procedure.
     *
     * @param labelPair a pair consisting of the label of a node {@code n} and the multiset of its neighbouring labels {@code m}
     * @return a compact hash (representing the combined node and neighbourhood labels)
     * @see <a href="https://www.jmlr.org/papers/v12/shervashidze11a.html">Shervashidze et al., "Weisfeiler-Lehman Graph Kernels" (2011)</a>
     */
    protected String hash(Pair<String, Multiset<String>> labelPair) {
        String prefix = labelPair.getLeft();
        Multiset<String> multisetNeighbors = labelPair.getRight();
        String compressedLabel = multisetNeighbors.stream()
                .sorted(Comparator
                        .<String, Integer>comparing(String::length) // First, compare by length
                        .thenComparing(Comparator.naturalOrder())) // then in lexicographic ascending order or String::compareTo
                .collect(Collectors.joining("$"));
        return prefix + "$" + compressedLabel + "#";
    }

    /**
     * Compresses the string signatures generated during a WL iteration into compact integer labels.
     * <p>
     * A running counter assigns a new integer label to each previously unseen signature. If a signature has
     * already been assigned a label, its existing label is reused. The resulting mapping provides consistent,
     * compact labels for subsequent refinement iterations.
     * <p>
     * This compression preserves permutation invariance, which is essential for aggregating neighbourhood
     * information in message-passing algorithms.
     *
     * @param inputList the string signatures to compress
     * @return the lookup table mapping string signatures to their compressed integer labels, represented as strings
     * @see <a href="https://www.jmlr.org/papers/v12/shervashidze11a.html">Shervashidze et al., "Weisfeiler-Lehman Graph Kernels" (2011), Section 2, p. 2543</a>
     */
    protected Map<String, String> hashesToIntegerLabel(List<String> inputList) {
        Set<String> collect = new TreeSet<>(Comparator
                .<String, Integer>comparing(String::length) // First, compare by length
                .thenComparing(Comparator.naturalOrder()));
        collect.addAll(inputList);
        for (String hash : collect) {
            hashToIntegerLabel(hash);
        }
        return hashToIntegerLabel;
    }

    protected String hashToIntegerLabel(String hash) {
        if (!hashToIntegerLabel.containsKey(hash)) {
            hashToIntegerLabel.put(hash, String.valueOf(runningInteger));
            runningInteger++;
            return hashToIntegerLabel.get(hash);
        }
        return hashToIntegerLabel.get(hash);
    }


    protected void updateLookupTable(Map<String, String> hashToIntegerLabel0) {
        LinkedHashMap<String, String> tmp = new LinkedHashMap<>(hashToIntegerLabel0);
        hashToIntegerLabel.clear();
        hashToIntegerLabel.putAll(tmp);
    }

    public IndexedHashMap<BigraphEntity<?>, String> getNodeColorMap() {
        return C;
    }

    public Map<Integer, Multiset<String>> getFeatureVector() {
        return featureVector;
    }

    public Map<String, String> getHashToIntegerLabel() {
        return hashToIntegerLabel;
    }

    protected void debugLog(String message) {
        if (debugEnabled) {
            System.out.println(message);
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }
}
