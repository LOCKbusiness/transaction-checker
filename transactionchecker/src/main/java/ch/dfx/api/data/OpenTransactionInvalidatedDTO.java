package ch.dfx.api.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionInvalidatedDTO {
  private String signature = null;
  private String reason = null;

  /**
   * 
   */
  public OpenTransactionInvalidatedDTO() {
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
