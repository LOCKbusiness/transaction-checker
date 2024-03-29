package ch.dfx.defichain.data.wallet;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiLoadWalletResultData extends ResultDataA {
  private DefiLoadWalletData result = null;

  /**
   * 
   */
  public DefiLoadWalletResultData() {
  }

  public DefiLoadWalletData getResult() {
    return result;
  }

  public void setResult(DefiLoadWalletData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
