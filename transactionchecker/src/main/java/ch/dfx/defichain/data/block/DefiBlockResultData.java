package ch.dfx.defichain.data.block;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiBlockResultData extends ResultDataA {
  private DefiBlockData result = null;

  /**
   * 
   */
  public DefiBlockResultData() {
  }

  public DefiBlockData getResult() {
    return result;
  }

  public void setResult(DefiBlockData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
