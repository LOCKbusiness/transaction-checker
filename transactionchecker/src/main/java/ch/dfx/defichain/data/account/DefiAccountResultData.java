package ch.dfx.defichain.data.account;

import java.util.List;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiAccountResultData extends ResultDataA {
  private List<String> result = null;

  /**
   * 
   */
  public DefiAccountResultData() {
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
