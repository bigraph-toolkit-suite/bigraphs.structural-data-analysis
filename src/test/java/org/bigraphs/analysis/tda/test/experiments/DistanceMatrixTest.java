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

import org.ejml.simple.SimpleMatrix;
import org.testng.annotations.Test;

public class DistanceMatrixTest {

    @Test
    public void test_distanceMatrix_in_EJML() {
        SimpleMatrix vectors = new SimpleMatrix(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});

        SimpleMatrix distanceMatrix = new SimpleMatrix(vectors.getNumRows(), vectors.getNumCols());

        System.out.println("Distance Matrix:");
        System.out.println(distanceMatrix);
    }
}
