package ch.dfx.defichain.data.basic;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiStringResultData extends ResultDataA {
  private String result = null;

  /**
   * 
   */
  public DefiStringResultData() {
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
