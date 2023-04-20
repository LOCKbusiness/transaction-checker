package ch.dfx.reporting.transparency.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryInterimDifferenceSheetDTOList extends ArrayList<HistoryInterimDifferenceSheetDTO> {
  private static final long serialVersionUID = -7859363872689019183L;

  /**
   * 
   */
  public HistoryInterimDifferenceSheetDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
