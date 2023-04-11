package ch.dfx.reporting.compare.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class APITransactionHistoryDTOList extends ArrayList<APITransactionHistoryDTO> {
  private static final long serialVersionUID = 6723453285362653554L;

  /**
   * 
   */
  public APITransactionHistoryDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
