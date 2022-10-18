package ch.dfx.api.data.withdrawal;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class PendingWithdrawalAssetDTO {
  private Integer id = null;

  private String name = null;
  private String displayName = null;

  private String category = null;
  private String blockchain = null;

  /**
   * 
   */
  public PendingWithdrawalAssetDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getBlockchain() {
    return blockchain;
  }

  public void setBlockchain(String blockchain) {
    this.blockchain = blockchain;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
