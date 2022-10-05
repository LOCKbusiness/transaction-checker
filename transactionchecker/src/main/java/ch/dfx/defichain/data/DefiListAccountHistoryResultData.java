package ch.dfx.defichain.data;

import java.util.List;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class DefiListAccountHistoryResultData extends ResultDataA {
  private List<DefiListAccountHistoryData> result = null;

  /**
   * 
   */
  public DefiListAccountHistoryResultData() {
  }

  public List<DefiListAccountHistoryData> getResult() {
    return result;
  }

  public void setResult(List<DefiListAccountHistoryData> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
