package ch.dfx.reporting.transparency.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryPriceSheetDTOList extends ArrayList<HistoryPriceSheetDTO> {
  private static final long serialVersionUID = -8780061669533069150L;

  /**
   * 
   */
  public HistoryPriceSheetDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
