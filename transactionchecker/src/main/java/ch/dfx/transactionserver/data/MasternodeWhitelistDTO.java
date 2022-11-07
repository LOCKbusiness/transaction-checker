package ch.dfx.transactionserver.data;

/**
 * 
 */
public class MasternodeWhitelistDTO extends DatabaseDTO {
  private final int walletId;
  private final int idx;
  private final String ownerAddress;

  // ...
  private String transactionId = null;
  private String operatorAddress = null;
  private String rewardAddress = null;

  private Integer creationBlockNumber = null;
  private Integer resignBlockNumber = null;

  private String state = null;

  /**
   * 
   */
  public MasternodeWhitelistDTO(
      int walletId,
      int idx,
      String ownerAddress) {
    this.walletId = walletId;
    this.idx = idx;
    this.ownerAddress = ownerAddress;
  }

  public int getWalletId() {
    return walletId;
  }

  public int getIdx() {
    return idx;
  }

  public String getOwnerAddress() {
    return ownerAddress;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
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

  public Integer getCreationBlockNumber() {
    return creationBlockNumber;
  }

  public void setCreationBlockNumber(Integer creationBlockNumber) {
    this.creationBlockNumber = creationBlockNumber;
  }

  public Integer getResignBlockNumber() {
    return resignBlockNumber;
  }

  public void setResignBlockNumber(Integer resignBlockNumber) {
    this.resignBlockNumber = resignBlockNumber;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
