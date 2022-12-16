package ch.dfx.transactionserver.data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 
 */
public class StakingWithdrawalReservedDTO extends DatabaseDTO {
  private final int tokenNumber;

  private Integer withdrawalId = null;
  private String transactionId = null;
  private String customerAddress = null;
  private BigDecimal vout = null;
  private Timestamp createTime = null;

  /**
   * 
   */
  public StakingWithdrawalReservedDTO(int tokenNumber) {
    this.tokenNumber = tokenNumber;
  }

  public int getTokenNumber() {
    return tokenNumber;
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

  public Timestamp getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Timestamp createTime) {
    this.createTime = createTime;
  }
}
