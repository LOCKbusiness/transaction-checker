package ch.dfx.defichain.data.wallet;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiVerifyMessageResultData extends ResultDataA {
  private Boolean result = null;

  /**
   * 
   */
  public DefiVerifyMessageResultData() {
  }

  public Boolean getResult() {
    return result;
  }

  public void setResult(Boolean result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
