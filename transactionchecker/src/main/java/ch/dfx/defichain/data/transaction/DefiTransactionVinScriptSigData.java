package ch.dfx.defichain.data.transaction;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class DefiTransactionVinScriptSigData {
  private String asm = null;
  private String hex = null;

  /**
   * 
   */
  public DefiTransactionVinScriptSigData() {
  }

  public String getAsm() {
    return asm;
  }

  public void setAsm(String asm) {
    this.asm = asm;
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
