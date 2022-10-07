package ch.dfx.defichain.data.wallet;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiSignMessageResultData extends ResultDataA {
  private String result = null;

  /**
   * 
   */
  public DefiSignMessageResultData() {
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
