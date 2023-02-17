package ch.dfx.defichain.data.pool;

import java.util.Map;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiPoolPairResultData extends ResultDataA {
  private Map<String, DefiPoolPairData> result = null;

  /**
   * 
   */
  public DefiPoolPairResultData() {
  }

  public Map<String, DefiPoolPairData> getResult() {
    return result;
  }

  public void setResult(Map<String, DefiPoolPairData> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
