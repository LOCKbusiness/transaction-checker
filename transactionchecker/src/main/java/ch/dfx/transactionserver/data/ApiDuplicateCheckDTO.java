package ch.dfx.transactionserver.data;

/**
 * 
 */
public class ApiDuplicateCheckDTO extends DatabaseDTO {
  private Integer withdrawalId = null;
  private String transactionId = null;

  /**
   * 
   */
  public ApiDuplicateCheckDTO() {
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
}
