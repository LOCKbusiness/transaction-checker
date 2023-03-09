package ch.dfx.tools.compare.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryDTOList extends ArrayList<HistoryDTO> {
  private static final long serialVersionUID = 6723453285362653554L;

  /**
   * 
   */
  public HistoryDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
