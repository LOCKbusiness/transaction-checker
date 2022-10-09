package ch.dfx.defichain.data.transaction;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiRawTransactionResultData extends ResultDataA {
  private String result = null;

  /**
   * 
   */
  public DefiRawTransactionResultData() {
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
