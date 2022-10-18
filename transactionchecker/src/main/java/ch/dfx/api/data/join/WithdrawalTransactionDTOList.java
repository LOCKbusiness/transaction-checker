package ch.dfx.api.data.join;

import java.util.ArrayList;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class WithdrawalTransactionDTOList extends ArrayList<WithdrawalTransactionDTO> {
  private static final long serialVersionUID = 7297440712035539058L;

  /**
   * 
   */
  public WithdrawalTransactionDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
