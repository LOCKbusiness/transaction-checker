package ch.dfx.api.data.withdrawal;

import java.math.BigDecimal;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class PendingWithdrawalDTO {
  private Integer id = null;

  private String signMessage = null;
  private String signature = null;
  private String asset = null;

  private BigDecimal amount = null;

  /**
   * 
   */
  public PendingWithdrawalDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getSignMessage() {
    return signMessage;
  }

  public void setSignMessage(String signMessage) {
    this.signMessage = signMessage;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getAsset() {
    return asset;
  }

  public void setAsset(String asset) {
    this.asset = asset;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
