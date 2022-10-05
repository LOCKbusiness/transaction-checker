package ch.dfx.defichain.data.transaction;

import java.math.BigDecimal;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class DefiTransactionVoutData {
  private String coinbase = null;
  private BigDecimal value = null;
  private Long n = null;
  private String tokenId = null;

  private DefiTransactionScriptPubKeyData scriptPubKey = null;

  /**
   * 
   */
  public DefiTransactionVoutData() {
  }

  public String getCoinbase() {
    return coinbase;
  }

  public void setCoinbase(String coinbase) {
    this.coinbase = coinbase;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public Long getN() {
    return n;
  }

  public void setN(Long n) {
    this.n = n;
  }

  public DefiTransactionScriptPubKeyData getScriptPubKey() {
    return scriptPubKey;
  }

  public void setScriptPubKey(DefiTransactionScriptPubKeyData scriptPubKey) {
    this.scriptPubKey = scriptPubKey;
  }

  public String getTokenId() {
    return tokenId;
  }

  public void setTokenId(String tokenId) {
    this.tokenId = tokenId;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
