package ch.dfx.transactionserver.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class MasternodeDTO {
  private int transactionNumber = -1;

  private int ownerAddressNumber = -1;
  private int operatorAddressNumber = -1;
  private int rewardAddressNumber = -1;

  private int creationBlockNumber = -1;
  private int resignBlockNumber = -1;

  private String state = null;

  /**
   * 
   */
  public MasternodeDTO() {
  }

  public int getTransactionNumber() {
    return transactionNumber;
  }

  public void setTransactionNumber(int transactionNumber) {
    this.transactionNumber = transactionNumber;
  }

  public int getOwnerAddressNumber() {
    return ownerAddressNumber;
  }

  public void setOwnerAddressNumber(int ownerAddressNumber) {
    this.ownerAddressNumber = ownerAddressNumber;
  }

  public int getOperatorAddressNumber() {
    return operatorAddressNumber;
  }

  public void setOperatorAddressNumber(int operatorAddressNumber) {
    this.operatorAddressNumber = operatorAddressNumber;
  }

  public int getRewardAddressNumber() {
    return rewardAddressNumber;
  }

  public void setRewardAddressNumber(int rewardAddressNumber) {
    this.rewardAddressNumber = rewardAddressNumber;
  }

  public int getCreationBlockNumber() {
    return creationBlockNumber;
  }

  public void setCreationBlockNumber(int creationBlockNumber) {
    this.creationBlockNumber = creationBlockNumber;
  }

  public int getResignBlockNumber() {
    return resignBlockNumber;
  }

  public void setResignBlockNumber(int resignBlockNumber) {
    this.resignBlockNumber = resignBlockNumber;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
