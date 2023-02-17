package ch.dfx.defichain.data.basic;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiBooleanResultData extends ResultDataA {
  private Boolean result = null;

  /**
   * 
   */
  public DefiBooleanResultData() {
  }

  public Boolean getResult() {
    return result;
  }

  public void setResult(Boolean result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
