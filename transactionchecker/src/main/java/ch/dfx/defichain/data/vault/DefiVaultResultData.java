package ch.dfx.defichain.data.vault;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiVaultResultData extends ResultDataA {
  private DefiVaultData result = null;

  /**
   * 
   */
  public DefiVaultResultData() {
  }

  public DefiVaultData getResult() {
    return result;
  }

  public void setResult(DefiVaultData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
