package ch.dfx.defichain.data.block;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiBlockHeaderResultData extends ResultDataA {
  private DefiBlockHeaderData result = null;

  /**
   * 
   */
  public DefiBlockHeaderResultData() {
  }

  public DefiBlockHeaderData getResult() {
    return result;
  }

  public void setResult(DefiBlockHeaderData result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
