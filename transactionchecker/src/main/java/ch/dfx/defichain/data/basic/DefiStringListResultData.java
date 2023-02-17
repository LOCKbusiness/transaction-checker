package ch.dfx.defichain.data.basic;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiStringListResultData extends ResultDataA {
  private List<String> result = null;

  /**
   * 
   */
  public DefiStringListResultData() {
  }

  public List<String> getResult() {
    return result;
  }

  public void setResult(List<String> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
