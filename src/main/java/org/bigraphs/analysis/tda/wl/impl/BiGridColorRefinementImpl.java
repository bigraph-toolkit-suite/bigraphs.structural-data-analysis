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
import java.util.stream.Collectors;
import org.bigraphs.analysis.tda.wl.AbstractWLColorRefinementProcedure;
import org.bigraphs.framework.core.BigraphEntityType;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.model.provider.spatial.signature.BiSpaceSignatureProvider;
import org.bigraphs.model.provider.spatial.signature.ThreeDimensionalBiSpaceSignatureProvider;

/**
 * This WL refinement specializes is for bigrid-style bigraph projections.
 * <p>
 * This class provides the specialization hooks required by
 * {@link AbstractWLColorRefinementProcedure}.
 *
 * @author Dominik Grzelak
 */
public class BiGridColorRefinementImpl extends AbstractWLColorRefinementProcedure {

    private final Set<String> localeControlNames;
    private final Set<String> routeControlNames;

    public BiGridColorRefinementImpl(PureBigraph bigraph) {
        this(
                bigraph,
                Set.of(BiSpaceSignatureProvider.LOCALE_TYPE),
                ThreeDimensionalBiSpaceSignatureProvider.getInstance().getSignature().getControls().stream().map(x -> x.getNamedType().stringValue()).collect(Collectors.toSet())
        );

    }

    public BiGridColorRefinementImpl(
            PureBigraph bigraph,
            Set<String> localeControlNames,
            Set<String> routeControlNames
    ) {
        super(bigraph);
        this.localeControlNames = localeControlNames;
        this.routeControlNames = routeControlNames;
    }

    @Override
    public List<BigraphEntity<?>> getAllRelevantBigraphEntities() {
        return bigraph.getNodes().stream()
                .filter(this::isLocaleEntity)
                .map(x -> (BigraphEntity<?>) x)
                .collect(Collectors.toList());
    }

    @Override
    public List<BigraphEntity<?>> getInterfaceBigraphEntities() {
        return Collections.emptyList();
    }

    @Override
    public List<BigraphEntity<?>> getOpenNeighborHoodOf(BigraphEntity<?> bigraphEntity) {
        if (bigraphEntity == null || !isLocaleEntity(bigraphEntity)) {
            return Collections.emptyList();
        }

        LinkedHashSet<BigraphEntity<?>> neighbors = new LinkedHashSet<>();

        // Traverse only route children of the locale
        for (BigraphEntity<?> child : bigraph.getChildrenOf(bigraphEntity)) {
            if (!isRouteEntity(child)) {
                continue;
            }

            for (BigraphEntity<?> targetLocale : resolveTargetLocales(child)) {
                if (!targetLocale.equals(bigraphEntity)) {
                    neighbors.add(targetLocale);
                }
            }
        }

        return new ArrayList<>(neighbors);
    }

    protected boolean isLocaleEntity(BigraphEntity<?> entity) {
        return BigraphEntityType.isNode(entity) && localeControlNames.contains(entity.getControl().getNamedType().stringValue());
    }

    protected boolean isRouteEntity(BigraphEntity<?> entity) {
        return BigraphEntityType.isNode(entity) && routeControlNames.contains(entity.getControl().getNamedType().stringValue());

    }

    protected List<BigraphEntity<?>> resolveTargetLocales(BigraphEntity<?> routeEntity) {
        if (routeEntity == null || !isRouteEntity(routeEntity)) {
            return Collections.emptyList();
        }

        LinkedHashSet<BigraphEntity<?>> result = new LinkedHashSet<>();

        for (BigraphEntity.Port routePort : bigraph.getPorts(routeEntity)) {
            BigraphEntity.Link link = bigraph.getLinkOfPoint(routePort);
            if (link == null) {
                continue;
            }

            for (BigraphEntity<?> point : bigraph.getPointsFromLink(link)) {
                if (!(point instanceof BigraphEntity.Port port)) {
                    continue;
                }

                BigraphEntity<?> node = bigraph.getNodeOfPort(port);
                if (node != null && node != routeEntity && isLocaleEntity(node)) {
                    result.add(node);
                }
            }
        }

        return new ArrayList<>(result);
    }
}