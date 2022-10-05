package ch.dfx.defichain.data.transaction;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiTransactionResultData extends ResultDataA {
  private DefiTransactionData result = null;

  /**
   * 
   */
  public DefiTransactionResultData() {
  }

  public DefiTransactionData getResult() {
    return result;
  }

  public void setResult(DefiTransactionData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
