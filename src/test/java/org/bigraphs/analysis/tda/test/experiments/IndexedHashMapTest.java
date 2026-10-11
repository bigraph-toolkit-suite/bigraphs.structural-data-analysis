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
package org.bigraphs.analysis.tda.test.experiments;

import org.bigraphs.analysis.tda.util.IndexedHashMap;
import org.testng.annotations.Test;

public class IndexedHashMapTest {
    @Test
    public void test_iteratedHashMap() {
        IndexedHashMap<String, Integer> indexedHashMap = new IndexedHashMap<>();

        // Iteration h
        indexedHashMap.addAt(0, "Key1", 10);
        indexedHashMap.addAt(0, "Key2", 20);
        indexedHashMap.addAt(0, "Key3", 30);

        // Access values from iteration h
        System.out.println("Value at Key2 in iteration h: " + indexedHashMap.getValueFromMapAt(0, "Key2"));

        // Iteration h+1
        indexedHashMap.addAt(1, "Key1", 15);
        indexedHashMap.addAt(1, "Key2", 25);
        indexedHashMap.addAt(1, "Key3", 35);

        // Access values from iteration h+1
        System.out.println("Value at Key2 in iteration h+1: " + indexedHashMap.getValueFromMapAt(1, "Key2"));
    }
}
