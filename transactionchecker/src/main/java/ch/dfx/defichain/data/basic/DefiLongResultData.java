package ch.dfx.defichain.data.basic;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiLongResultData extends ResultDataA {
  private Long result = null;

  /**
   * 
   */
  public DefiLongResultData() {
  }

  public Long getResult() {
    return result;
  }

  public void setResult(Long result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
