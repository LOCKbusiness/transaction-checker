package ch.dfx.api.data.transaction;

import java.util.ArrayList;
import java.util.Collection;

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

  /**
   * 
   */
  public OpenTransactionDTOList(Collection<OpenTransactionDTO> collection) {
    super(collection);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
