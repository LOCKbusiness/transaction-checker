package ch.dfx.ocean.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionDetailScriptDTO {
  private String type = null;
  private String hex = null;

  /**
   * 
   */
  public TransactionDetailScriptDTO() {
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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
