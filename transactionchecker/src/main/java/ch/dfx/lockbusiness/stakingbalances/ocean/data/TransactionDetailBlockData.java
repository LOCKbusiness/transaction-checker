package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionDetailBlockData {
  private String hash = null;
  private Long height = null;

  /**
   * 
   */
  public TransactionDetailBlockData() {
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Long getHeight() {
    return height;
  }

  public void setHeight(Long height) {
    this.height = height;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
