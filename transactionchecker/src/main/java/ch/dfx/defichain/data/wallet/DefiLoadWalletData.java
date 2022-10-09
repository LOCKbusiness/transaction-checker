package ch.dfx.defichain.data.wallet;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DefiLoadWalletData {
  private String name = null;
  private String warning = null;

  /**
   * 
   */
  public DefiLoadWalletData() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getWarning() {
    return warning;
  }

  public void setWarning(String warning) {
    this.warning = warning;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
