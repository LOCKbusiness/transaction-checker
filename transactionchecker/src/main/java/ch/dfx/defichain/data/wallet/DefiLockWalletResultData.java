package ch.dfx.defichain.data.wallet;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiLockWalletResultData extends ResultDataA {
  private String result = null;

  /**
   * 
   */
  public DefiLockWalletResultData() {
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
