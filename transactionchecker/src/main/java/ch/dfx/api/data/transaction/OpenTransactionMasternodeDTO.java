package ch.dfx.api.data.transaction;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionMasternodeDTO {
  private String transactionId = null;

  private String ownerAddress = null;
  private String operatorAddress = null;
  private String rewardAddress = null;

  /**
   * 
   */
  public OpenTransactionMasternodeDTO() {
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(String ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public String getOperatorAddress() {
    return operatorAddress;
  }

  public void setOperatorAddress(String operatorAddress) {
    this.operatorAddress = operatorAddress;
  }

  public String getRewardAddress() {
    return rewardAddress;
  }

  public void setRewardAddress(String rewardAddress) {
    this.rewardAddress = rewardAddress;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
