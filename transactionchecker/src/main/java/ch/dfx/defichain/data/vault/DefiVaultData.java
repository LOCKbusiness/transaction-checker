package ch.dfx.defichain.data.vault;

import java.math.BigDecimal;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DefiVaultData {
  private String vaultId = null;
  private String ownerAddress = null;

  private BigDecimal collateralValue = null;
  private BigDecimal loanValue = null;
  private BigDecimal interestValue = null;

  private BigDecimal informativeRatio = null;
  private Integer collateralRatio = null;

  /**
   * 
   */
  public DefiVaultData() {
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

  public BigDecimal getCollateralValue() {
    return collateralValue;
  }

  public void setCollateralValue(BigDecimal collateralValue) {
    this.collateralValue = collateralValue;
  }

  public BigDecimal getLoanValue() {
    return loanValue;
  }

  public void setLoanValue(BigDecimal loanValue) {
    this.loanValue = loanValue;
  }

  public BigDecimal getInterestValue() {
    return interestValue;
  }

  public void setInterestValue(BigDecimal interestValue) {
    this.interestValue = interestValue;
  }

  public BigDecimal getInformativeRatio() {
    return informativeRatio;
  }

  public void setInformativeRatio(BigDecimal informativeRatio) {
    this.informativeRatio = informativeRatio;
  }

  public Integer getCollateralRatio() {
    return collateralRatio;
  }

  public void setCollateralRatio(Integer collateralRatio) {
    this.collateralRatio = collateralRatio;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
