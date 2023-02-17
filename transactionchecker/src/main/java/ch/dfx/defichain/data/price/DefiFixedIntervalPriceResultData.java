package ch.dfx.defichain.data.price;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiFixedIntervalPriceResultData extends ResultDataA {
  private DefiFixedIntervalPriceData result = null;

  /**
   * 
   */
  public DefiFixedIntervalPriceResultData() {
  }

  public DefiFixedIntervalPriceData getResult() {
    return result;
  }

  public void setResult(DefiFixedIntervalPriceData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
