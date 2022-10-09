package ch.dfx.ocean.data;

import java.math.BigDecimal;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionsDetailDTO {
  private String id = null;
  private String hid = null;
  private String type = null;
  private String typeHex = null;
  private String txid = null;

  private TransactionDetailBlockDTO block = null;
  private TransactionDetailScriptDTO script = null;

  private TransactionDetailVinDTO vin = null;
  private TransactionDetailVoutDTO vout = null;

  private BigDecimal value = null;
  private Long tokenId = null;

  /**
   * 
   */
  public TransactionsDetailDTO() {
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

  public TransactionDetailBlockDTO getBlock() {
    return block;
  }

  public void setBlock(TransactionDetailBlockDTO block) {
    this.block = block;
  }

  public TransactionDetailScriptDTO getScript() {
    return script;
  }

  public void setScript(TransactionDetailScriptDTO script) {
    this.script = script;
  }

  public TransactionDetailVinDTO getVin() {
    return vin;
  }

  public void setVin(TransactionDetailVinDTO vin) {
    this.vin = vin;
  }

  public TransactionDetailVoutDTO getVout() {
    return vout;
  }

  public void setVout(TransactionDetailVoutDTO vout) {
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
    return TransactionCheckerUtils.toJson(this);
  }
}
