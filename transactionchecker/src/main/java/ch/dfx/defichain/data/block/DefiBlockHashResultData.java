package ch.dfx.defichain.data.block;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiBlockHashResultData extends ResultDataA {
  private String result = null;

  /**
   * 
   */
  public DefiBlockHashResultData() {
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
