package ch.dfx.reporting.transparency.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryAssetPriceSheetDTOList extends ArrayList<HistoryAssetPriceSheetDTO> {
  private static final long serialVersionUID = -917411282951127824L;

  /**
   * 
   */
  public HistoryAssetPriceSheetDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
