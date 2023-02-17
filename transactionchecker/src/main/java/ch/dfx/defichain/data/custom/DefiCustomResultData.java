package ch.dfx.defichain.data.custom;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiCustomResultData extends ResultDataA {
  private DefiCustomData result = null;

  /**
   * 
   */
  public DefiCustomResultData() {
  }

  public DefiCustomData getResult() {
    return result;
  }

  public void setResult(DefiCustomData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
