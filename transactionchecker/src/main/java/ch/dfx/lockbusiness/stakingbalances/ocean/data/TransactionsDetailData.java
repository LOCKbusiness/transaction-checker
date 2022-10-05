package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import java.math.BigDecimal;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class TransactionsDetailData {
  private String id = null;
  private String hid = null;
  private String type = null;
  private String typeHex = null;
  private String txid = null;

  private TransactionDetailBlockData block = null;
  private TransactionDetailScriptData script = null;

  private TransactionDetailVinData vin = null;
  private TransactionDetailVoutData vout = null;

  private BigDecimal value = null;
  private Long tokenId = null;

  /**
   * 
   */
  public TransactionsDetailData() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getHid() {
    return hid;
  }

  public void setHid(String hid) {
    this.hid = hid;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTypeHex() {
    return typeHex;
  }

  public void setTypeHex(String typeHex) {
    this.typeHex = typeHex;
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public TransactionDetailBlockData getBlock() {
    return block;
  }

  public void setBlock(TransactionDetailBlockData block) {
    this.block = block;
  }

  public TransactionDetailScriptData getScript() {
    return script;
  }

  public void setScript(TransactionDetailScriptData script) {
    this.script = script;
  }

  public TransactionDetailVinData getVin() {
    return vin;
  }

  public void setVin(TransactionDetailVinData vin) {
    this.vin = vin;
  }

  public TransactionDetailVoutData getVout() {
    return vout;
  }

  public void setVout(TransactionDetailVoutData vout) {
    this.vout = vout;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public Long getTokenId() {
    return tokenId;
  }

  public void setTokenId(Long tokenId) {
    this.tokenId = tokenId;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
