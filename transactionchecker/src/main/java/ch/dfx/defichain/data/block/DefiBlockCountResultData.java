package ch.dfx.defichain.data.block;

import ch.dfx.common.PayoutManagerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiBlockCountResultData extends ResultDataA {
  private Long result = null;

  /**
   * 
   */
  public DefiBlockCountResultData() {
  }

  public Long getResult() {
    return result;
  }

  public void setResult(Long result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
