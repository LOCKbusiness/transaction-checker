package ch.dfx.api.data.transaction;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionPayloadDTO {
  // ...
  private Integer id = null;
  private String type = null;
  private String assetType = null;

  // ...
  private String ownerWallet = null;
  private Integer accountIndex = null;

  /**
   * 
   */
  public OpenTransactionPayloadDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAssetType() {
    return assetType;
  }

  public void setAssetType(String assetType) {
    this.assetType = assetType;
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

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
