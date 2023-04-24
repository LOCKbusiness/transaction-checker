package ch.dfx.ocean.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

public class HistoryPoolDTOList extends ArrayList<HistoryPoolDTO> {
  private static final long serialVersionUID = 1005184608428603682L;

  /**
   * 
   */
  public HistoryPoolDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
