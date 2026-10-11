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
package org.bigraphs.analysis.tda.wl.impl;

import java.util.*;
import org.bigraphs.analysis.tda.wl.AbstractWLColorRefinementProcedure;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;

/**
 * The 1-dimensional WL stabilisation algorithm is a color refinement or canonical labelling algorithm developed as a test for graph isomorphism.
 * This class implements the algorithm for a place graph of a bigraph.
 *
 * @author Dominik Grzelak
 */
public class PlaceGraphWLColorRefinementImpl extends AbstractWLColorRefinementProcedure {

    public PlaceGraphWLColorRefinementImpl(PureBigraph bigraph) {
        super(bigraph);
    }

    /**
     * Roots, nodes and sites in this order.
     * @return
     */
    @Override
    public List<BigraphEntity<?>> getAllRelevantBigraphEntities() {
        return bigraph.getAllPlaces();
    }

    /**
     * only roots and sites
     * @return
     */
    @Override
    public List<BigraphEntity<?>> getInterfaceBigraphEntities() {
        List<BigraphEntity<?>> collect = new ArrayList<>();
        collect.addAll(bigraph.getRoots().stream().map(x -> (BigraphEntity<?>)x).toList());
        collect.addAll(bigraph.getSites().stream().map(x -> (BigraphEntity<?>)x).toList());
        return collect;
    }

    @Override
    public List<BigraphEntity<?>> getOpenNeighborHoodOf(BigraphEntity<?> bigraphEntity) {
        return bigraph.getOpenNeighborhoodOfNode(bigraphEntity);
    }
}
