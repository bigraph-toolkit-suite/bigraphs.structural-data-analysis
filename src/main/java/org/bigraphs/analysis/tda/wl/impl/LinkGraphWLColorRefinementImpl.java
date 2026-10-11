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
import org.bigraphs.framework.core.BigraphEntityType;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DynamicControl;

/**
 * The 1-dimensional WL stabilisation algorithm is a color refinement or canonical labelling algorithm developed as a test for graph isomorphism.
 * This class implements the algorithm for a link graph of a bigraph.
 * <p>
 * The inner and outer names are considered as nodes here as well.
 * The edge (i.e., closed link) is regarded as the edge connecting nodes as the parent map in the place graph.
 * Since the procedure generates simple integer labels, the "semantics" of the true labels is considered (string equality).
 *
 * @author Dominik Grzelak
 */
public class LinkGraphWLColorRefinementImpl extends AbstractWLColorRefinementProcedure {


    // For link graph iso check they do not matter, but one may decide to include them as well
    boolean includeRoots = true;

    public LinkGraphWLColorRefinementImpl(PureBigraph bigraph) {
        super(bigraph);
    }

    @Override
    public List<BigraphEntity<?>> getAllRelevantBigraphEntities() {
        List<BigraphEntity<?>> collect = new ArrayList<>();
        if (includeRoots)
            collect.addAll(bigraph.getRoots().stream().map(x -> (BigraphEntity<?>) x).toList()); // to identify linkage across roots, for proper iso test needed
        collect.addAll(bigraph.getOuterNames().stream().map(x -> (BigraphEntity<?>) x).toList());
        collect.addAll(bigraph.getNodes().stream().map(x -> (BigraphEntity<?>) x).toList());
        collect.addAll(bigraph.getInnerNames().stream().map(x -> (BigraphEntity<?>) x).toList());
        return collect;
    }

    @Override
    public List<BigraphEntity<?>> getInterfaceBigraphEntities() {
        List<BigraphEntity<?>> collect = new ArrayList<>();
        collect.addAll(bigraph.getOuterNames().stream().map(x -> (BigraphEntity<?>) x).toList());
        collect.addAll(bigraph.getInnerNames().stream().map(x -> (BigraphEntity<?>) x).toList());
        return collect;
    }

    /**
     * Get all neighboring nodes of a node in the link graph.
     * Neighborhood nodes can be only of types returned by {@link #getAllRelevantBigraphEntities()}.
     * The order is important and different implementations cannot be mixed when comparing two graphs. It belongs to the procedure.
     * (Here, outer names, inner names, and nodes.)
     *
     * @param bigraphEntity
     * @return
     */
    @Override
    public List<BigraphEntity<?>> getOpenNeighborHoodOf(BigraphEntity<?> bigraphEntity) {
        Set<BigraphEntity<?>> collect = new HashSet<>();

        // Nodes
        if (BigraphEntityType.isNode(bigraphEntity)) {
            // Add also its top-level root for iso test
            // Reason: it could be that there exists a substructure somewhere exactly like this under a different root.
            // But then they are not similar when doing iso check on the complete bigraph; while for the link graph only it doesn't matter
            if (includeRoots)
                collect.add(bigraph.getTopLevelRoot(bigraphEntity));

            Collection<BigraphEntity.Link> incidentLinksOf = bigraph.getIncidentLinksOf((BigraphEntity.NodeEntity<?>) bigraphEntity);
            //filter outernames: they are direct neighbors
            incidentLinksOf.stream().filter(x -> BigraphEntityType.isOuterName(x)).forEach(x -> {
                collect.add(x);
            });
            // filter edges: closed links represent the prnt map of the place graph.
            // they are not node elements of a bigraph in this case.
            // We need to check if these closed links, link to other nodes or interfaces
            incidentLinksOf.stream().filter(x -> BigraphEntityType.isEdge(x)).forEach(x -> {
                List<BigraphEntity<?>> pointsFromEdge = bigraph.getPointsFromLink(x);
                pointsFromEdge.stream().filter(BigraphEntityType::isInnerName).forEach(inner -> {
                    collect.add(inner);
                });
                pointsFromEdge.stream().filter(BigraphEntityType::isPort).forEach(port -> {
                    BigraphEntity.NodeEntity<DynamicControl> nodeOfPort = bigraph.getNodeOfPort((BigraphEntity.Port) port);
                    if (nodeOfPort != null && !nodeOfPort.equals(bigraphEntity)) {
                        collect.add(nodeOfPort);
                    }
                });
            });
        }

        // Outer names
        if (BigraphEntityType.isOuterName(bigraphEntity)) {
            List<BigraphEntity<?>> pointsFromLink = bigraph.getPointsFromLink((BigraphEntity.OuterName) bigraphEntity);
            pointsFromLink.stream().filter(BigraphEntityType::isInnerName).forEach(inner -> {
                collect.add(inner);
            });
            pointsFromLink.stream().filter(BigraphEntityType::isPort).forEach(port -> {
                BigraphEntity.NodeEntity<DynamicControl> nodeOfPort = bigraph.getNodeOfPort((BigraphEntity.Port) port);
                if (nodeOfPort != null) {
                    collect.add(nodeOfPort);
                    // and its top-level root for iso test
                    if (includeRoots)
                        collect.add(bigraph.getTopLevelRoot(nodeOfPort));
                }
            });
        }

        // Inner names
        if (BigraphEntityType.isInnerName(bigraphEntity)) {
            BigraphEntity.Link linkOfInner = bigraph.getLinkOfPoint(bigraphEntity);
            if (BigraphEntityType.isOuterName(linkOfInner))
                collect.add(linkOfInner);
            if (BigraphEntityType.isEdge(linkOfInner)) {
                List<BigraphEntity<?>> pointsFromEdge = bigraph.getPointsFromLink(linkOfInner);
                pointsFromEdge.stream().filter(BigraphEntityType::isInnerName)
                        .filter(x -> !bigraphEntity.equals(x)).forEach(inner -> {
                            collect.add(inner);
                        });
                pointsFromEdge.stream().filter(BigraphEntityType::isPort).forEach(port -> {
                    BigraphEntity.NodeEntity<DynamicControl> nodeOfPort = bigraph.getNodeOfPort((BigraphEntity.Port) port);
                    if (nodeOfPort != null) {
                        collect.add(nodeOfPort);
                        // and its top-level root
                        if (includeRoots)
                            collect.add(bigraph.getTopLevelRoot(nodeOfPort));
                    }
                });
            }

        }
        // edges are not considered
        return new ArrayList<>(collect); //Collections.emptyList();
    }

    /**
     * Default is false
     */
    public boolean isIncludeRoots() {
        return includeRoots;
    }

    /**
     * Default is false
     */
    public void setIncludeRoots(boolean includeRoots) {
        this.includeRoots = includeRoots;
    }
}
