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
import com.google.common.hash.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.testng.annotations.Test;

public class GuavaHashingTest {

    @Test
    public void test_hash_function_guava() {
        HashFunction hashFunction = Hashing.sipHash24(); //Hashing.sha256();

        String input = "Hello, Guava!";
        HashCode hashCode = hashFunction.hashString(input, StandardCharsets.UTF_8);

        System.out.println("Hash: " + hashCode.toString());
    }

    /**
     * Not reliable: just calling hashCode()
     */
    @Test
    public void test_hash_of_multiset() {
        Multiset<Integer> multiset = HashMultiset.create();
        multiset.add(1);
        multiset.add(2);
        multiset.add(2);
        multiset.add(3);
        multiset.add(4);
        System.out.println(multiset.hashCode());
        Multiset<Integer> multiset2 = HashMultiset.create();
        multiset2.add(1);
        multiset2.add(2);
        multiset2.add(2);
        multiset2.add(3);
        multiset2.add(4);
        System.out.println(multiset2.hashCode());


        Multiset<Integer> multiset3 = HashMultiset.create();
        multiset3.addAll(Arrays.asList(2, 3, 4, 5));
        System.out.println(multiset3.hashCode());
        multiset3.add(1); // doesnt work: same hash
        System.out.println(multiset3.hashCode());

        System.out.println("ab".hashCode());
        System.out.println("ba".hashCode());
    }

    /**
     * Custom hash function: does not really compute unique hashes when not working with integers.
     * The element count of a multiset has to be considered (see test below).
     * This might work here only for integers
     */
    @Test
    public void test_hash_function_guava_object() {
        HashFunction hashFunction = Hashing.sipHash24(); //Hashing.sha256();
        Multiset<Integer> multiset = HashMultiset.create();
        multiset.addAll(Arrays.asList(1, 2, 2, 3, 4));

        Funnel<Multiset<Integer>> multisetFunnel = new Funnel<Multiset<Integer>>() {
            @Override
            public void funnel(Multiset<Integer> from, PrimitiveSink into) {
                from.entrySet().forEach(x -> {
                    into.putInt(x.getElement());
                });
            }
        };
        HashCode hashCode = hashFunction.hashObject(multiset, multisetFunnel);
        System.out.println("Hash: " + hashCode.toString());

        Multiset<Integer> multiset2 = HashMultiset.create();
        multiset2.addAll(Arrays.asList(1, 2, 2, 3, 4));
        hashCode = hashFunction.hashObject(multiset2, multisetFunnel);
        System.out.println("Hash: " + hashCode.toString());

        Multiset<Integer> multiset3 = HashMultiset.create();
        multiset3.addAll(Arrays.asList(2, 3, 4, 5));
        hashCode = hashFunction.hashObject(multiset3, multisetFunnel);
        System.out.println("Hash: " + hashCode.toString());
        multiset3.add(1);
        hashCode = hashFunction.hashObject(multiset3, multisetFunnel);
        System.out.println("Hash: " + hashCode.toString());
    }

    /**
     * Working custom hashing function for multiset objects: Creates unique hashes
     * Takes element name and occurrence count into consideration.
     * The hashes can be converted to unique integer labels in each iteration of the WL procedure
     */
    @Test
    public void test_hash_function_guava_object_string() {
        HashFunction hashFunction = Hashing.murmur3_32_fixed();
        Funnel<Multiset<String>> multisetFunnel = new Funnel<Multiset<String>>() {
            @Override
            public void funnel(Multiset<String> from, PrimitiveSink into) {
                from.entrySet().forEach(x -> {
                    // using only the element doesnt work, the count is needed
                    into.putString(x.getElement() + ":" + x.getCount(), StandardCharsets.UTF_8);
                });
            }
        };

        Multiset<String> multiset = HashMultiset.create();
//        multiset.addAll(Arrays.asList("Computer", "Person", "Room"));
        multiset.addAll(Arrays.asList("Computer", "Person", "Room"));
        HashCode hashCode = hashFunction.hashObject(multiset, multisetFunnel);
        System.out.println("Hash multiset 1: " + hashCode.toString());

        Multiset<String> multiset2 = HashMultiset.create();
        multiset2.addAll(Arrays.asList("Person", "Computer", "Room"));
        hashCode = hashFunction.hashObject(multiset2, multisetFunnel);
        System.out.println("Hash multiset 2: " + hashCode.toString());

        Multiset<String> multiset3 = HashMultiset.create();
        multiset3.addAll(Arrays.asList("Computer", "Computer", "Person", "Room"));
        hashCode = hashFunction.hashObject(multiset3, multisetFunnel);
        System.out.println("Hash multiset 3: " + hashCode.toString());

        System.out.println("---");
        System.out.println("hashedMultisetToIntegerLabel for multiset 3");
        hashedMultisetToIntegerLabel(multiset3);
        System.out.println("---");
        System.out.println("adding 'Person' control to multiset 3");
        multiset3.add("Person");
        hashCode = hashFunction.hashObject(multiset3, multisetFunnel);
        System.out.println("Hash multiset 3: " + hashCode.toString());

        System.out.println("---");
        System.out.println("hashedMultisetToIntegerLabel for multiset 3");
        hashedMultisetToIntegerLabel(multiset3);
        System.out.println("---");
    }

    public Map<String, String> hashedMultisetToIntegerLabel(Multiset<String> inputList) {
        Map<String, String> hashToName = new HashMap<>();
        List<String> outputList = new ArrayList<>();

        int runningInteger = 1;
        for (String hash : inputList) {
            if (!hashToName.containsKey(hash)) {
                hashToName.put(hash, String.valueOf(runningInteger));
                runningInteger++;
            }
            String assignedName = hashToName.get(hash);
            // debug output
            outputList.add("Hash '" + assignedName + "': " + hash);
        }

        // debug output
        for (String entry : outputList) {
            System.out.println(entry);
        }

        return hashToName;
    }


}
