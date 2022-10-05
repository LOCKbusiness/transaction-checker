package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class TransactionDetailVoutData {
  private String txid = null;
  private Long n = null;

  /**
   * 
   */
  public TransactionDetailVoutData() {
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
    return PayoutManagerUtils.toJson(this);
  }
}
