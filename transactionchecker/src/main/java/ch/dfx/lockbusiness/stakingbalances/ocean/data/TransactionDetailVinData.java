package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionDetailVinData {
  private String txid = null;
  private Long n = null;

  /**
   * 
   */
  public TransactionDetailVinData() {
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public Long getN() {
    return n;
  }

  public void setN(Long n) {
    this.n = n;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
