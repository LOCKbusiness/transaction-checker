package ch.dfx.api.data.transaction;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionVerifiedDTO {
  private String signature = null;

  /**
   * 
   */
  public OpenTransactionVerifiedDTO() {
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
