package ch.dfx.defichain.data;

import java.util.List;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class DefiAccountResultData extends ResultDataA {
  private List<DefiAmountData> result = null;

  /**
   * 
   */
  public DefiAccountResultData() {
  }

  public List<DefiAmountData> getResult() {
    return result;
  }

  public void setResult(List<DefiAmountData> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
