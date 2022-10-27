package ch.dfx.transactionserver.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class MasternodeWhitelistDTO {
  private int walletId = -1;
  private int idx = -1;
  private String ownerAddress = null;

  /**
   * 
   */
  public MasternodeWhitelistDTO() {
  }

  public int getWalletId() {
    return walletId;
  }

  public void setWalletId(int walletId) {
    this.walletId = walletId;
  }

  public int getIdx() {
    return idx;
  }

  public void setIdx(int idx) {
    this.idx = idx;
  }

  public String getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(String ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
