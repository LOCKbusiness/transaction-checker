package ch.dfx.defichain.data.transaction;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiTransactionData extends ResultDataA {
  private String txid = null;
  private String hash = null;

  private List<DefiTransactionVinData> vin = null;
  private List<DefiTransactionVoutData> vout = null;

  private String hex = null;

  /**
   * 
   */
  public DefiTransactionData() {
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public List<DefiTransactionVinData> getVin() {
    return vin;
  }

  public void setVin(List<DefiTransactionVinData> vin) {
    this.vin = vin;
  }

  public List<DefiTransactionVoutData> getVout() {
    return vout;
  }

  public void setVout(List<DefiTransactionVoutData> vout) {
    this.vout = vout;
  }

  public String getHex() {
    return hex;
  }

  public void setHex(String hex) {
    this.hex = hex;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
