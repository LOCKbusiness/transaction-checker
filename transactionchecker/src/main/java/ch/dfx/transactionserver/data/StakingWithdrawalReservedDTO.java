package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

/**
 * 
 */
public class StakingWithdrawalReservedDTO extends DatabaseDTO {
  private Integer withdrawalId = null;
  private String transactionId = null;
  private String customerAddress = null;
  private BigDecimal vout = null;

  /**
   * 
   */
  public StakingWithdrawalReservedDTO() {
  }

  public Integer getWithdrawalId() {
    return withdrawalId;
  }

  public void setWithdrawalId(Integer withdrawalId) {
    this.withdrawalId = withdrawalId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getCustomerAddress() {
    return customerAddress;
  }

  public void setCustomerAddress(String customerAddress) {
    this.customerAddress = customerAddress;
  }

  public BigDecimal getVout() {
    return vout;
  }

  public void setVout(BigDecimal vout) {
    this.vout = vout;
  }
}
