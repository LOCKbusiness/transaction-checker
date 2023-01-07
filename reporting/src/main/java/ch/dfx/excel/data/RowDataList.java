package ch.dfx.excel.data;

import java.util.ArrayList;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class RowDataList extends ArrayList<RowData> {
  private static final long serialVersionUID = 4941141532109209586L;

  private final int rowOffset;

  /**
   * 
   */
  public RowDataList(int rowOffset) {
    this.rowOffset = rowOffset;
  }

  public int getRowOffset() {
    return rowOffset;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
