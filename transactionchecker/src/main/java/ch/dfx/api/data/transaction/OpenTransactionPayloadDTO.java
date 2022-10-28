package ch.dfx.api.data.transaction;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionPayloadDTO {
  // Masternode ...
  private String ownerWallet = null;
  private Integer accountIndex = null;

  // Withdrawal ...
  private Integer id = null;

  /**
   * 
   */
  public OpenTransactionPayloadDTO() {
  }

  public String getOwnerWallet() {
    return ownerWallet;
  }

  public void setOwnerWallet(String ownerWallet) {
    this.ownerWallet = ownerWallet;
  }

  public Integer getAccountIndex() {
    return accountIndex;
  }

  public void setAccountIndex(Integer accountIndex) {
    this.accountIndex = accountIndex;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
