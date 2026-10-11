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

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.simulation.modelchecking.BigraphFilter;
import org.bigraphs.framework.simulation.modelchecking.SimulatedAnnealingFrontierStrategy;
import org.bigraphs.framework.visualization.BigraphGraphvizExporter;
import org.ejml.dense.row.NormOps_DDRM;

/**
 * @author Dominik Grzelak
 */
public class WLAnnealingFrontierStrategy extends SimulatedAnnealingFrontierStrategy<PureBigraph> {

    private final int wlIterations;

    protected int maxHeightTarget;
    protected int maxWidthTarget;
    protected int maxNodes;
    protected BigraphFilter<PureBigraph> filter = BigraphFilter.noop();
    protected boolean exportFinalState = false;
    protected Optional<PureBigraph> lastState = Optional.empty();


    public WLAnnealingFrontierStrategy(
            List<PureBigraph> goalExemplars,
            double initialTemperature,
            int nEpoch,
            int fairnessK,
            int wlIterations
    ) {
        super(goalExemplars, initialTemperature, nEpoch, fairnessK);
        this.wlIterations = wlIterations;
        maxHeightTarget = goalExemplars.stream().map(this::computeMaxHeightTree).max(Integer::compareTo).orElseGet(() -> 0);
        maxWidthTarget = goalExemplars.stream().map(this::computeMaxTreeWidth).max(Integer::compareTo).get();
        maxNodes = goalExemplars.stream().map(this::computeNodeCount).max(Integer::compareTo).get();
    }

    public void setEnergyEps(double energyEps0) {
        this.energyEps = energyEps0;
    }

    @Override
    protected double graphKernel(PureBigraph graphA, PureBigraph graphB) {
        if (graphA == null || graphB == null) return 0.0;

        WeisfeilerLehmanGraphKernel kernel = new WeisfeilerLehmanGraphKernel(graphA, graphB);

        double value = NormOps_DDRM.inducedP2(
                kernel.computeGraphKernel(wlIterations)
                        .getKernelMatrix()
                        .getLeft() // only place graph
                        .getDDRM()
        );

        // WL similarities should be non-negative
        return Math.max(0.0, value);
    }

    @Override
    protected void stopProcedure(PureBigraph chosen) {
        super.stopProcedure(chosen);
        double e = energy(chosen);
        System.out.println("Stop at energy: " + e + " (Threshold: " + energyEps + ")");
        System.out.println("Occurrence Counter = " + occurrenceCounter);
        System.out.println("State Count = " + modelChecker.getReactionGraph().getGraph().vertexSet().size());
        lastState = Optional.ofNullable(chosen);
        if (isExportFinalState()) {
            try {
                BigraphGraphvizExporter.toPNG(chosen, true, new File("chosen.png"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Optional<PureBigraph> getLastState() {
        return lastState;
    }

    public int computeMaxTreeWidth(PureBigraph state) {
        return state.getNodes().stream().map(state::getChildrenOf).max(Comparator.comparingInt(List::size)).orElse(List.of()).size();
    }

    public int computeNodeCount(PureBigraph state) {
        return state.getAllPlaces().size();
    }

    public int computeMaxHeightTree(PureBigraph state) {

        return state.getNodes().stream().max(Comparator.comparingInt(state::getLevelOf)).map(state::getLevelOf).orElse(0);
    }

    public int getMaxHeightTarget() {
        return maxHeightTarget;
    }

    public void setMaxHeightTarget(int maxHeightTarget) {
        this.maxHeightTarget = maxHeightTarget;
    }

    public int getMaxWidthTarget() {
        return maxWidthTarget;
    }

    public void setMaxWidthTarget(int maxWidthTarget) {
        this.maxWidthTarget = maxWidthTarget;
    }

    public int getMaxNodes() {
        return maxNodes;
    }

    public void setMaxNodes(int maxNodes) {
        this.maxNodes = maxNodes;
    }

    public void setDefaultFilter() {
        setFilter(DEFAULT_FILTER);
    }

    public void setFilter(BigraphFilter<PureBigraph> filter) {
        this.filter = filter;
    }

    public final BigraphFilter<PureBigraph> DEFAULT_FILTER = bigraph -> {
        if (bigraph == null) return null;

        int heightOfTree = computeMaxHeightTree(bigraph);
        if (heightOfTree > maxHeightTarget) return null;

        int widthOfTree = computeMaxTreeWidth(bigraph);
        if (widthOfTree > maxWidthTarget) return null;

        int nc = computeNodeCount(bigraph);
        if (nc > maxNodes) return null;

        return bigraph;
    };

    public boolean isExportFinalState() {
        return exportFinalState;
    }

    public void setExportFinalState(boolean exportFinalState) {
        this.exportFinalState = exportFinalState;
    }
}
