package ch.dfx.api.data.withdrawal;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class PendingWithdrawalDTOList extends ArrayList<PendingWithdrawalDTO> {
  private static final long serialVersionUID = -1332732599396715030L;

  /**
   * 
   */
  public PendingWithdrawalDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
