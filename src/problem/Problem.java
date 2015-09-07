/*
 * This file is part of the genetic-sudoku-solver.
 *
 * (c) Marcel Moosbrugger
 *
 * This source file is subject to the MIT license that is bundled
 * with this source code in the file LICENSE.
 */

package problem;

import sudoku.SudokuGrid;

import java.util.*;

/**
 * Represents the problem. Takes an unfinished sudoku-grid
 * and preprocesses different things
 */
public class Problem {

    private SudokuGrid grid;
    private Set<Integer> variableFields;
    private Map<Integer, Integer[]> variableFieldsForRows;
    private Map<Integer, Integer[]> variableFieldsForColumns;
    private Map<Integer, Integer[]> variableFieldsForBlocks;
    private Map<Integer, Set<Integer>> validNumbers;

    /**
     * Default constructor. Takes an unfinished sudoku-grid and
     * preprocesses different things
     * @param grid The unfinished sudoku-grid which represents the problem
     */
    public Problem(SudokuGrid grid) {
        this.grid = grid;
        this.preprocess();
    }

    /**
     * @return variableFields
     */
    public Integer[] getVariableFields() {
        return this.variableFields.toArray(new Integer[this.variableFields.size()]);
    }

    /**
     * @param row the index of the row to get the variable fields for
     * @return an array of variable fields in a given row
     */
    public Integer[] getVariableFieldsForRow(int row) {
        return this.variableFieldsForRows.get(row);
    }

    /**
     * @param column the index of the column to get the variable fields for
     * @return an array of variable fields in a given column
     */
    public Integer[] getVariableFieldsForColumn(int column) {
        return this.variableFieldsForColumns.get(column);
    }

    /**
     * @param block the index of the block to get the variable fields for
     * @return an array of variable fields in a given block
     */
    public Integer[] getVariableFieldsForBlock(int block) {
        return this.variableFieldsForBlocks.get(block);
    }

    /**
     * @param index the index to return the valid numbers for
     * @return array of valid numbers for a given index
     */
    public Integer[] getValidNumbersForIndex(int index) {
        return this.validNumbers.get(index).toArray(new Integer[this.validNumbers.get(index).size()]); // TODO make better (not performant)
    }

    /**
     * @param index the index of the field
     * @param number the number which gets tested on validity
     * @return true iff a given number is valid for a field with a given index
     */
    public boolean numberIsValidForIndex(int index, int number) {
        return this.validNumbers.get(index).contains(number);
    }

    /**
     * Preprocesses different things
     */
    private void preprocess() {
        this.preprocessVariableFields();
        this.presolveGrid();
        this.preprocessVariableFieldsUnits();
    }

    /**
     * Calculates an array of indexes of fields which have to be filled
     */
    private void preprocessVariableFields() {
        this.variableFields = this.grid.getEmptyFields();
    }

    /**
     * Calculates for each row, column and block which fields need to be filled
     */
    private void preprocessVariableFieldsUnits() {
        this.variableFieldsForRows = new Hashtable<>();
        this.variableFieldsForColumns = new Hashtable<>();
        this.variableFieldsForBlocks = new Hashtable<>();

        for (int unit = 0; unit < this.grid.getSideLength(); unit++) {
            ArrayList<Integer> variableFieldsForRow = new ArrayList<>();
            ArrayList<Integer> variableFieldsForColumn = new ArrayList<>();
            ArrayList<Integer> variableFieldsForBlock = new ArrayList<>();

            for (int i = 0; i < this.grid.getSideLength(); i++) {

                int rowField = this.grid.getIndexByRow(unit, i);
                if (this.variableFields.contains(rowField)) {
                    variableFieldsForRow.add(rowField);
                }

                int columnField = this.grid.getIndexByColumn(unit, i);
                if (this.variableFields.contains(columnField)) {
                    variableFieldsForColumn.add(columnField);
                }

                int blockField = this.grid.getIndexByBlock(unit, i);
                if (this.variableFields.contains(blockField)) {
                    variableFieldsForBlock.add(blockField);
                }
            }

            this.variableFieldsForRows.put(unit, variableFieldsForRow.toArray(new Integer[variableFieldsForRow.size()]));
            this.variableFieldsForColumns.put(unit, variableFieldsForColumn.toArray(new Integer[variableFieldsForColumn.size()]));
            this.variableFieldsForBlocks.put(unit, variableFieldsForBlock.toArray(new Integer[variableFieldsForBlock.size()]));
        }
    }

    /**
     * Calculates an array which for each index on the problem-grid
     * holds an array of numbers which don't interfere with the already fixed
     * number on the problems grid
     */
    private void preprocessValidNumbers() {
        this.validNumbers = new Hashtable<>();
        for (int index : this.getVariableFields()) {
            Set<Integer> validNumbers = new HashSet<>();
            for (int i = this.grid.getValidMin(); i <= this.grid.getValidMax(); i++) {
                this.grid.write(index, i);
                if (this.grid.getConflicts() == 0) {
                    validNumbers.add(i);
                }
                this.grid.write(index, 0);
            }
            if (validNumbers.size() == 1) {
                this.grid.write(index, validNumbers.iterator().next());
                this.variableFields.remove(index);
            } else {
                this.validNumbers.put(index, validNumbers);
            }
        }
    }

    /**
     * Presolves the grid with the determined numbers
     */
    private void presolveGrid() {
        boolean gridChanged = true;
        while (gridChanged) {
            this.preprocessValidNumbers();
            gridChanged = this.insertFixedFields();
        }
    }

    /**
     * Takes each variable field and looks if one of it's valid numbers is unique among its block, row or column.
     * If so insert the valid number into the grid
     * @return true iff a number for a field has been inserted
     */
    private boolean insertFixedFields() {
        Iterator<Integer> iterator = this.variableFields.iterator();
        while (iterator.hasNext()) {
            int index = iterator.next();

            Set<Integer> validNumbersCopy = new HashSet<>(this.validNumbers.get(index));
            int[] rowIndices = this.grid.getRowForIndex(index);
            this.removeDoubleValidNumbers(index, validNumbersCopy, rowIndices);
            int[] columnIndices = this.grid.getColumnForIndex(index);
            this.removeDoubleValidNumbers(index, validNumbersCopy, columnIndices);
            int[] blockIndices = this.grid.getBlockForIndex(index);
            this.removeDoubleValidNumbers(index, validNumbersCopy, blockIndices);
            if (validNumbersCopy.size() == 1) {
                this.grid.write(index, validNumbersCopy.iterator().next());
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    /**
     * Intersects a set of valid numbers with all valid numbers from a given unit
     * @param index The index which belongs to the passed valid numbers
     * @param validNumbers the set of valid numbers to bisect with all the valid numbers of a given unit
     * @param unit the unit to take the rest of the valid numbers to all bisect with the passed valid numbers set
     */
    private void removeDoubleValidNumbers(int index, Set<Integer> validNumbers, int[] unit) {
        for (int unitIndex: unit) {
            if (unitIndex != index && this.variableFields.contains(unitIndex)) {
                Iterator<Integer> iterator = validNumbers.iterator();
                while (iterator.hasNext()) {
                    int validNumber = iterator.next();
                    if (this.validNumbers.get(unitIndex).contains(validNumber)) {
                        iterator.remove();
                    }
                }
            }
        }
    }
}
