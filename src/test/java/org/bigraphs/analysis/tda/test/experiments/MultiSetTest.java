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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import org.testng.annotations.Test;

public class MultiSetTest {

    @Test
    public void test_multiset_in_Guava() {
        Multiset<String> multiset = HashMultiset.create();

        // Add elements to multiset
        multiset.add("apple");
        multiset.add("banana");
        multiset.add("apple");
        multiset.add("orange");
        multiset.add("apple");

        // Count occurrences of elements
        System.out.println("Count of apple: " + multiset.count("apple"));
        System.out.println("Count of banana: " + multiset.count("banana"));
        System.out.println("Count of orange: " + multiset.count("orange"));
    }

    /**
     * Example:
     * Let A = {l, l, m, m, n, n, n, n}
     * B = {l, m, m, m, n},
     * A ∪ B = {l, l, m, m, m, n, n, n, n}
     */
    @Test
    public void test_union_of_multisets() {
        // Multiset A
        Multiset<String> A = HashMultiset.create();
        A.add("l", 2);
        A.add("m", 2);
        A.add("n", 4);

        // Multiset B
        Multiset<String> B = HashMultiset.create();
        B.add("l", 1);
        B.add("m", 3);
        B.add("n", 1);

        // Union of A and B
//        Multiset<String> union = HashMultiset.create();
//        union.addAll(A);
//        union.addAll(B);
        Multiset<String> union = Multisets.union(A, B);

        System.out.println("Multiset A: " + A);
        System.out.println("Multiset B: " + B);
        System.out.println("Union of A and B: " + union);
    }
}
