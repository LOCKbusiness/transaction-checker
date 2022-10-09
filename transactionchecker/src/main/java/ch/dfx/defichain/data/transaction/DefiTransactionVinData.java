package ch.dfx.defichain.data.transaction;

import java.util.List;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DefiTransactionVinData {
  private String coinbase = null;
  private String txid = null;
  private Long vout = null;
  private DefiTransactionVinScriptSigData scriptSig = null;
  private List<String> txinwitness = null;

  /**
   * 
   */
  public DefiTransactionVinData() {
  }

  public String getCoinbase() {
    return coinbase;
  }

  public void setCoinbase(String coinbase) {
    this.coinbase = coinbase;
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public Long getVout() {
    return vout;
  }

  public void setVout(Long vout) {
    this.vout = vout;
  }

  public DefiTransactionVinScriptSigData getScriptSig() {
    return scriptSig;
  }

  public void setScriptSig(DefiTransactionVinScriptSigData scriptSig) {
    this.scriptSig = scriptSig;
  }

  public List<String> getTxinwitness() {
    return txinwitness;
  }

  public void setTxinwitness(List<String> txinwitness) {
    this.txinwitness = txinwitness;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
