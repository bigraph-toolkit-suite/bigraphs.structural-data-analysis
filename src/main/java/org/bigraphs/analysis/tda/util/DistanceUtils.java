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

import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.simple.SimpleMatrix;

/**
 * @author Dominik Grzelak
 */
public class DistanceUtils {

    public static double frobeniusSimilarity(SimpleMatrix A, SimpleMatrix B) {
//        SimpleMatrix difference = A.minus(B);
//        double frobeniusNormDiff = NormOps_DDRM.inducedP2(difference.getDDRM());
        double frobeniusNormA = NormOps_DDRM.inducedP2(A.getDDRM());
        double frobeniusNormB = NormOps_DDRM.inducedP2(B.getDDRM());
        double similarity = Math.sqrt(Math.pow(frobeniusNormA, 2) + Math.pow(frobeniusNormB, 2));
        return similarity;
    }

    public static double frobeniusSimilarityPadded(SimpleMatrix A, SimpleMatrix B) {
        SimpleMatrix matrixA = new SimpleMatrix(A);
        SimpleMatrix matrixB = new SimpleMatrix(B);

        // Resize matrices if they have different dimensions
        int numRowsA = matrixA.numRows();
        int numColsA = matrixA.numCols();
        int numRowsB = matrixB.numRows();
        int numColsB = matrixB.numCols();

        if (numRowsA != numRowsB || numColsA != numColsB) {
            int maxRows = Math.max(numRowsA, numRowsB);
            int maxCols = Math.max(numColsA, numColsB);

            matrixA = resizeMatrix(matrixA, maxRows, maxCols);
            matrixB = resizeMatrix(matrixB, maxRows, maxCols);
        }

        SimpleMatrix difference = matrixA.minus(matrixB);
        double frobeniusNormDiff = NormOps_DDRM.inducedP2(difference.getDDRM());
        double frobeniusNormA = NormOps_DDRM.inducedP2(matrixA.getDDRM());
        double frobeniusNormB = NormOps_DDRM.inducedP2(matrixB.getDDRM());

        double similarity = -frobeniusNormDiff / Math.sqrt(Math.pow(frobeniusNormA, 2) + Math.pow(frobeniusNormB, 2));
        return similarity;
    }

    public static SimpleMatrix resizeMatrix(SimpleMatrix matrix, int numRows, int numCols) {
        SimpleMatrix resizedMatrix = new SimpleMatrix(numRows, numCols);
        resizedMatrix.insertIntoThis(0, 0, matrix);
        return resizedMatrix;
    }
}
