package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

/**
 * 
 */
public class VaultWhitelistDTO extends DatabaseDTO {
  private final String vaultId;
  private final String address;

  private BigDecimal minRatio = null;
  private BigDecimal maxRatio = null;

  private String state = null;

  /**
   * 
   */
  public VaultWhitelistDTO(
      String vaultId,
      String address) {
    this.vaultId = vaultId;
    this.address = address;
  }

  public String getVaultId() {
    return vaultId;
  }

  public String getAddress() {
    return address;
  }

  public BigDecimal getMinRatio() {
    return minRatio;
  }

  public void setMinRatio(BigDecimal minRatio) {
    this.minRatio = minRatio;
  }

  public BigDecimal getMaxRatio() {
    return maxRatio;
  }

  public void setMaxRatio(BigDecimal maxRatio) {
    this.maxRatio = maxRatio;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
