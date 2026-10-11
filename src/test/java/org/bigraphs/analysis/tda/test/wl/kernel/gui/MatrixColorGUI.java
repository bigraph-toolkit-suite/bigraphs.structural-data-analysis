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
package org.bigraphs.analysis.tda.test.wl.kernel.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Usage:
 * <pre>
 *         double[][] data = {
 *                 {2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
 *                 {1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
 *                 {0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0},
 *                 {0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0},
 *                 {0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0},
 *                 {0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0},
 *                 {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0},
 *                 {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0}
 *         };
 *
 *         String[] columnNames = {"Col1", "Col2", "Col3", "Col4", "Col5", "Col6", "Col7", "Col8"};
 *
 *         SwingUtilities.invokeLater(() -> new MatrixColorGUI(data, columnNames));
 * </pre>
 */
public class MatrixColorGUI extends JFrame {

    public MatrixColorGUI(double[][] data, String[] columnNames) {
        this(data, columnNames, "Graph Kernel Matrix Color Display");
    }

    public MatrixColorGUI(double[][] data, String[] columnNames, String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JTable table = new JTable(new MatrixTableModel(data, columnNames));
        double maxVal = getMaxValue(data);
        table.setDefaultRenderer(Object.class, new ColorTableCellRenderer(maxVal));
        add(new JScrollPane(table));
        pack();
        setVisible(true);
    }

    private static Object[][] castDoubleArrayToObjectArray(double[][] doubleArray) {
        int rows = doubleArray.length;
        int cols = doubleArray[0].length;

        Object[][] objectArray = new Object[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                objectArray[i][j] = doubleArray[i][j];
            }
        }

        return objectArray;
    }

    // Custom TableModel for the matrix
    private static class MatrixTableModel extends DefaultTableModel { //AbstractTableModel
        public MatrixTableModel(double[][] data, String[] columnNames) {
            super(castDoubleArrayToObjectArray(data), (Object[]) columnNames);
        }
    }

    // Custom TableCellRenderer to display colors based on values
    private static class ColorTableCellRenderer extends DefaultTableCellRenderer {
        double maxVal;

        public ColorTableCellRenderer(double maxVal) {
            this.maxVal = maxVal;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // Set background color based on the value (scaled to 255)
            if (value instanceof Number) {
                double doubleValue = ((Number) value).doubleValue();
                int intValue = (int) (255 * doubleValue / this.maxVal);

                Color color = new Color(intValue, 0, 0);
                cellComponent.setBackground(color);
            }

            return cellComponent;
        }
    }

    private static double getMaxValue(double[][] data) {
        double maxValue = Double.MIN_VALUE;

        for (double[] row : data) {
            for (double value : row) {
                if (value > maxValue) {
                    maxValue = value;
                }
            }
        }

        return maxValue;
    }
}
