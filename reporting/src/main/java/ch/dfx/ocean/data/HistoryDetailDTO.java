package ch.dfx.ocean.data;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryDetailDTO {
  private String owner = null;
  private String txid = null;
  private int txn = -1;
  private String type = null;

  private List<String> amounts = null;
  private HistoryBlockDTO block = null;

  /**
   * 
   */
  public HistoryDetailDTO() {
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public int getTxn() {
    return txn;
  }

  public void setTxn(int txn) {
    this.txn = txn;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getAmounts() {
    return amounts;
  }

  public void setAmounts(List<String> amounts) {
    this.amounts = amounts;
  }

  public HistoryBlockDTO getBlock() {
    return block;
  }

  public void setBlock(HistoryBlockDTO block) {
    this.block = block;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
