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
import org.bigraphs.analysis.tda.wl.impl.LinkGraphWLColorRefinementImpl;
import org.bigraphs.analysis.tda.wl.impl.PlaceGraphWLColorRefinementImpl;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;

/**
 * Compares two bigraphs using the one-dimensional Weisfeiler–Lehman (1-WL) color-refinement procedure,
 * also known as naive vertex refinement or color refinement.
 * <p>
 * The comparison applies color refinement separately to the place graphs and link graphs of the supplied
 * bigraphs. The results are combined: both comparisons must succeed for {@link #isoCheck()} to return {@code true}.
 * <p>
 * The refinement procedure stops when the coloring stabilizes or the iteration limit is reached. Different color
 * distributions establish non-isomorphism.
 * <p>
 * Note that the 1-WL procedure is not a complete graph-isomorphism test.
 * If this checker returns {@code true}, the refinement procedure has not distinguished the supplied bigraphs; this does
 * not guarantee that they are isomorphic.
 * <p>
 * The shared-label refinement approach can also be generalized to compare a set of graphs, although this
 * implementation compares a pair of bigraphs.
 *
 * @author Dominik Grzelak
 * @see AbstractWLColorRefinementProcedure
 * @see <a href="https://www.jmlr.org/papers/v12/shervashidze11a.html">
 * Shervashidze et al., "Weisfeiler-Lehman Graph Kernels" (2011), JMLR 12, 2539–2561</a>
 * @see <a href="https://doi.org/10.1007/s41109-019-0195-3">
 * Kriege, Johansson, and Morris, "A Survey on Graph Kernels" (2020)</a>
 */
public class BWLIsomorphismChecker {
    PlaceGraphWLColorRefinementImpl wlPlaceGraphA, wlPlaceGraphB;
    LinkGraphWLColorRefinementImpl wlLinkGraphA, wlLinkGraphB;

    PureBigraph bigraphA;
    PureBigraph bigraphB;

    protected final Map<String, String> hashToIntegerLabel = new LinkedHashMap<>();
    int runningInteger = 1;

    public BWLIsomorphismChecker(PureBigraph bigraphA, PureBigraph bigraphB) {
        this.bigraphA = bigraphA;
        this.bigraphB = bigraphB;
        this.wlPlaceGraphA = new PlaceGraphWLColorRefinementImpl(bigraphA);
        this.wlPlaceGraphB = new PlaceGraphWLColorRefinementImpl(bigraphB);
        this.wlLinkGraphA = new LinkGraphWLColorRefinementImpl(bigraphA);
        this.wlLinkGraphB = new LinkGraphWLColorRefinementImpl(bigraphB);
    }

    /**
     * Combines the output of {@link #isoCheckByPlaceGraph()} and {@link #isoCheckByLinkGraph()} by
     * combining them logically with the AND operator and evaluating the result.
     *
     * @return true if both supplied bigraphs are isomorphic (approximate test); otherwise false.
     */
    public boolean isoCheck() {
        return isoCheckByPlaceGraph() && isoCheckByLinkGraph();
    }


    /**
     * Checks whether the link graph of two bigraphs are isomorphic.
     *
     * @return true if both supplied link graphs are isomorphic (approximate test); otherwise false.
     */
    public boolean isoCheckByLinkGraph() {
        // If both link graphs are empty, they are the same
        if (bigraphA.getAllLinks().size() + bigraphA.getInnerNames().size() == 0 &&
                bigraphB.getAllLinks().size() + bigraphB.getInnerNames().size() == 0) {
            return true;
        }

        return isoCheckFor(wlLinkGraphA, wlLinkGraphB);
    }

    /**
     * Checks whether the place graph of two bigraphs are isomorphic.
     *
     * @return true if both supplied place graphs are isomorphic (approximate test); otherwise false.
     */
    public boolean isoCheckByPlaceGraph() {
        // If both place graphs are empty, they are the same
        if (bigraphA.getAllPlaces().isEmpty() && bigraphB.getAllPlaces().isEmpty()) {
            return true;
        }

        return isoCheckFor(wlPlaceGraphA, wlPlaceGraphB);
    }

    public boolean isoCheckFor(AbstractWLColorRefinementProcedure wlA, AbstractWLColorRefinementProcedure wlB) {
        int bigraphDim;
        // if length of relevant node set differ, both bigraphs are not isomorphic
        if ((bigraphDim = checkEqualityBigraphDimensions()) == -1) {
            return false;
        }
        int maxIteration = bigraphDim + 1;

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

        // Bigraphs are labeled graphs so controls play a significant role
        // At this point the initial hashToIntegerLabel map must be similar;
        // otherwise they do not have the same amount of node labels and can be regarded as non-isomorphic already.
        // Quit here if the node label sets of the two bigraphs differ
        if (!wlA.previousColors.equals(wlB.previousColors)) {
            return false;
        }
        if (!wlA.getHashToIntegerLabel().equals(wlB.getHashToIntegerLabel())) {
            return false;
        }
        for (int h = 1; h < maxIteration; h++) {
            final int hOld = h - 1;
            int finalH = h;

            /// (2-3) Step (2-3): Sorting each Multiset and Label compression
            Map<BigraphEntity, String> bigraphEntityToHashA = wlA.step_2and3(hOld, finalH);
            Map<BigraphEntity, String> bigraphEntityToHashB = wlB.step_2and3(hOld, finalH);

            LinkedList<String> allGeneratedHashes = new LinkedList<>(bigraphEntityToHashA.values());
            allGeneratedHashes.addAll(bigraphEntityToHashB.values());
            Map<String, String> hashToIntLookupTable = hashesToIntegerLabel(allGeneratedHashes);

            wlA.updateLookupTable(hashToIntLookupTable);
            wlB.updateLookupTable(hashToIntLookupTable);
            assert wlA.getHashToIntegerLabel().equals(wlB.getHashToIntegerLabel());
            wlA.step4(bigraphEntityToHashA, finalH);
            wlB.step4(bigraphEntityToHashB, finalH);


            Multiset<String> currentColorsA = wlA.getFeatureVector().get(finalH);
            Multiset<String> currentColorsB = wlB.getFeatureVector().get(finalH);
            int currentIterationA = wlA.finalStep(currentColorsA, finalH);
            int currentIterationB = wlB.finalStep(currentColorsB, finalH);
            if ((currentIterationA != currentIterationB)) {
                return false;
            }
            //they stopped at same iteration count
            if (currentIterationA == -1 || currentIterationB == -1) {
                continue;
            }
            return currentColorsA.equals(currentColorsB);
        }
        return false;
    }

    public Map<String, String> hashesToIntegerLabel(List<String> inputList) {
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
        return hashToIntegerLabel;
    }

    protected int checkEqualityBigraphDimensions() {
        int bigraphADim = bigraphA.getAllPlaces().size() + bigraphA.getAllLinks().size() + bigraphA.getInnerNames().size();
        int bigraphBDim = bigraphB.getAllPlaces().size() + bigraphB.getAllLinks().size() + bigraphB.getInnerNames().size();
        if (bigraphADim == bigraphBDim) {
            return bigraphADim;
        }
        return -1;
    }
}
