package ch.dfx.excel.data;

import java.util.ArrayList;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class CellDataList extends ArrayList<CellData> {
  private static final long serialVersionUID = -5128911485588295763L;

  /**
   * 
   */
  public CellDataList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
