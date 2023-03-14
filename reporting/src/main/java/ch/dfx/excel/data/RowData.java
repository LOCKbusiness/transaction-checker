package ch.dfx.excel.data;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;

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

  public RowData addCellData(@Nonnull CellData cellData) {
    cellDataList.add(cellData);

    return this;
  }

  public RowData addCellDataList(@Nonnull CellDataList cellDataList) {
    this.cellDataList.addAll(cellDataList);

    return this;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
