package ch.dfx.defichain.data.network;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class PeerInfoResultData extends ResultDataA {
  private List<PeerInfoData> result = null;

  /**
   * 
   */
  public PeerInfoResultData() {
  }

  public List<PeerInfoData> getResult() {
    return result;
  }

  public void setResult(List<PeerInfoData> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
