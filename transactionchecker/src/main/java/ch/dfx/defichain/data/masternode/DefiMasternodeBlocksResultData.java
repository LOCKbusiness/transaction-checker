package ch.dfx.defichain.data.masternode;

import java.util.Map;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiMasternodeBlocksResultData extends ResultDataA {
  private Map<String, String> result = null;

  /**
   * 
   */
  public DefiMasternodeBlocksResultData() {
  }

  public Map<String, String> getResult() {
    return result;
  }

  public void setResult(Map<String, String> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
