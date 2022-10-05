package ch.dfx.defichain.data;

import java.util.List;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class DefiListAccountHistoryData {
  private String owner = null;

  private Long blockHeight = null;
  private String blockHash = null;
  private String type = null;
  private Long txn = null;
  private String txid = null;
  private List<DefiAmountData> amounts = null;

  /**
   * 
   */
  public DefiListAccountHistoryData() {
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Long getBlockHeight() {
    return blockHeight;
  }

  public void setBlockHeight(Long blockHeight) {
    this.blockHeight = blockHeight;
  }

  public String getBlockHash() {
    return blockHash;
  }

  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Long getTxn() {
    return txn;
  }

  public void setTxn(Long txn) {
    this.txn = txn;
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public List<DefiAmountData> getAmounts() {
    return amounts;
  }

  public void setAmounts(List<DefiAmountData> amounts) {
    this.amounts = amounts;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
