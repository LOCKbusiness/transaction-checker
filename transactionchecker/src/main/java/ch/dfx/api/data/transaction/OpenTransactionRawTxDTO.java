package ch.dfx.api.data.transaction;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionRawTxDTO {
  private String hex = null;

  /**
   * 
   */
  public OpenTransactionRawTxDTO() {
  }

  public String getHex() {
    return hex;
  }

  public void setHex(String hex) {
    this.hex = hex;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
