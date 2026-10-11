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
package org.bigraphs.analysis.tda.util;

import java.util.*;

/**
 * A map implementation that stores indexed key-value pairs.
 *
 * @param <V> type of the value
 * @param <K> type of the key
 * @author Dominik Grzelak
 */
public class IndexedHashMap<V, K> {
    private List<Map<V, K>> indexMap;

    public IndexedHashMap() {
        this.indexMap = new LinkedList<>();
    }

    public void addAt(int ix, V key, K value) {
        Map<V, K> hashMap = getOrCreateMapAt(ix);
        hashMap.put(key, value);
    }

    public K getValueFromMapAt(int ix, V key) {
        Map<V, K> hashMap = getMapAt(ix);
        return (hashMap != null) ? hashMap.getOrDefault(key, null) : null;
    }

    private Map<V, K> getOrCreateMapAt(int ix) {
        if (ix < 0) {
            throw new IllegalArgumentException("Iteration argument must be non-negative (>= 0).");
        }

        while (indexMap.size() <= ix) {
            indexMap.add(new LinkedHashMap<>());
        }

        return indexMap.get(ix);
    }

    private Map<V, K> getMapAt(int ix) {
        if (ix < 0 || ix >= indexMap.size()) {
            return null; // Iteration out of bounds
        }

        return indexMap.get(ix);
    }

    public int sizeAt(int ix) {
        if (getMapAt(ix) == null) return -1;
        return Objects.requireNonNull(getMapAt(ix)).size();
    }

    public Set<V> keySetAt(int ix) {
        if (getMapAt(ix) == null) return new LinkedHashSet<>();
        return Objects.requireNonNull(getMapAt(ix)).keySet();
    }

    public Set<Map.Entry<V, K>> entrySetAt(int iteration) {
        if (getMapAt(iteration) == null) return new LinkedHashSet<>();
        return Objects.requireNonNull(getMapAt(iteration)).entrySet();
    }
}
