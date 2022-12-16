package ch.dfx.excel.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class CellData {
  private int rowIndex = -1;
  private int cellIndex = -1;

  private Object value = null;

  private boolean bold = false;

  /**
   * 
   */
  public CellData() {
  }

  public int getRowIndex() {
    return rowIndex;
  }

  public CellData setRowIndex(int rowIndex) {
    this.rowIndex = rowIndex;

    return this;
  }

  public int getCellIndex() {
    return cellIndex;
  }

  public CellData setCellIndex(int cellIndex) {
    this.cellIndex = cellIndex;

    return this;
  }

  public Object getValue() {
    return value;
  }

  public CellData setValue(Object value) {
    this.value = value;

    return this;
  }

  public boolean isBold() {
    return bold;
  }

  public CellData setBold(boolean bold) {
    this.bold = bold;

    return this;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
