package ch.dfx.excel.data;

import javax.annotation.Nonnull;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class RowData {

  private final CellDataList cellDataList;

  /**
   * 
   */
  public RowData() {
    cellDataList = new CellDataList();
  }

  public CellDataList getCellDataList() {
    return cellDataList;
  }

  public void addCellData(@Nonnull CellData cellData) {
    cellDataList.add(cellData);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
