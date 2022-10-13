package ch.dfx.defichain.data.masternode;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiMasternodeData extends ResultDataA {

  private String ownerAuthAddress = null;
  private String operatorAuthAddress = null;
  private String rewardAddress = null;

  private Long creationHeight = null;
  private Long resignHeight = null;

  private String state = null;

  private String message = null;

  /**
   * 
   */
  public DefiMasternodeData() {
  }

  public String getOwnerAuthAddress() {
    return ownerAuthAddress;
  }

  public void setOwnerAuthAddress(String ownerAuthAddress) {
    this.ownerAuthAddress = ownerAuthAddress;
  }

  public String getOperatorAuthAddress() {
    return operatorAuthAddress;
  }

  public void setOperatorAuthAddress(String operatorAuthAddress) {
    this.operatorAuthAddress = operatorAuthAddress;
  }

  public String getRewardAddress() {
    return rewardAddress;
  }

  public void setRewardAddress(String rewardAddress) {
    this.rewardAddress = rewardAddress;
  }

  public Long getCreationHeight() {
    return creationHeight;
  }

  public void setCreationHeight(Long creationHeight) {
    this.creationHeight = creationHeight;
  }

  public Long getResignHeight() {
    return resignHeight;
  }

  public void setResignHeight(Long resignHeight) {
    this.resignHeight = resignHeight;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
