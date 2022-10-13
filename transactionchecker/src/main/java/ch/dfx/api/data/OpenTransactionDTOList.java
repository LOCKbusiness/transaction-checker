package ch.dfx.api.data;

import java.util.ArrayList;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionDTOList extends ArrayList<OpenTransactionDTO> {
  private static final long serialVersionUID = -5625983490239149873L;

  /**
   * 
   */
  public OpenTransactionDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
