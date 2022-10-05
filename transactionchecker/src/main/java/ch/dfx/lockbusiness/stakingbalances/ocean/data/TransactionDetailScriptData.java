package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class TransactionDetailScriptData {
  private String type = null;
  private String hex = null;

  /**
   * 
   */
  public TransactionDetailScriptData() {
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
    return PayoutManagerUtils.toJson(this);
  }
}
