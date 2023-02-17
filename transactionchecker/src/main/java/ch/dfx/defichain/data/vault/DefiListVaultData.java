package ch.dfx.defichain.data.vault;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class DefiListVaultData {

  private String vaultId = null;
  private String ownerAddress = null;

  private String loanSchemeId = null;
  private String state = null;

  /**
   * 
   */
  public DefiListVaultData() {
  }

  public String getVaultId() {
    return vaultId;
  }

  public void setVaultId(String vaultId) {
    this.vaultId = vaultId;
  }

  public String getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(String ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public String getLoanSchemeId() {
    return loanSchemeId;
  }

  public void setLoanSchemeId(String loanSchemeId) {
    this.loanSchemeId = loanSchemeId;
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
