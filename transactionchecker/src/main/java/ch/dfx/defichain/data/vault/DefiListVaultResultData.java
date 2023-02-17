package ch.dfx.defichain.data.vault;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiListVaultResultData extends ResultDataA {
  private List<DefiListVaultData> result = null;

  /**
   * 
   */
  public DefiListVaultResultData() {
  }

  public List<DefiListVaultData> getResult() {
    return result;
  }

  public void setResult(List<DefiListVaultData> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
