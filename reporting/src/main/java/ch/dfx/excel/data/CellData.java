package ch.dfx.excel.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class CellData {

  private Object value = null;

  private boolean bold = false;

  /**
   * 
   */
  public CellData() {
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
