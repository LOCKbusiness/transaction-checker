package ch.dfx.reporting.compare.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class APIStakingBalanceDTOList extends ArrayList<APIStakingBalanceDTO> {
  private static final long serialVersionUID = -8542883976447885239L;

  /**
   * 
   */
  public APIStakingBalanceDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
