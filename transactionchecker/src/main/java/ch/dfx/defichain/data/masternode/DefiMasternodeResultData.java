package ch.dfx.defichain.data.masternode;

import java.util.Map;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiMasternodeResultData extends ResultDataA {
  private Map<String, DefiMasternodeData> result = null;

  /**
   * 
   */
  public DefiMasternodeResultData() {
  }

  public Map<String, DefiMasternodeData> getResult() {
    return result;
  }

  public void setResult(Map<String, DefiMasternodeData> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
