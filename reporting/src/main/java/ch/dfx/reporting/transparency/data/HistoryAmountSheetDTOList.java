package ch.dfx.reporting.transparency.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryAmountSheetDTOList extends ArrayList<HistoryAmountSheetDTO> {
  private static final long serialVersionUID = -6767380497775983382L;

  /**
   * 
   */
  public HistoryAmountSheetDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
