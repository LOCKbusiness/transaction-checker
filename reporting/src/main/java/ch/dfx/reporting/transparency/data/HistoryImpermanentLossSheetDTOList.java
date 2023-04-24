package ch.dfx.reporting.transparency.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryImpermanentLossSheetDTOList extends ArrayList<HistoryImpermanentLossSheetDTO> {
  private static final long serialVersionUID = -9155238087593598024L;

  /**
   * 
   */
  public HistoryImpermanentLossSheetDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
