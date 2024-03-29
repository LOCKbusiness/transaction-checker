package ch.dfx.api.data.join;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;

/**
 * 
 */
public class TransactionWithdrawalDTO {
  private Integer id = null;

  private OpenTransactionDTO openTransactionDTO = null;
  private PendingWithdrawalDTO pendingWithdrawalDTO = null;

  // ...
  private String customerAddress = null;

  private String stateReason = null;
  private TransactionWithdrawalStateEnum state = TransactionWithdrawalStateEnum.OPEN;

  /**
   * 
   */
  public TransactionWithdrawalDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public OpenTransactionDTO getOpenTransactionDTO() {
    return openTransactionDTO;
  }

  public void setOpenTransactionDTO(OpenTransactionDTO openTransactionDTO) {
    this.openTransactionDTO = openTransactionDTO;
  }

  public PendingWithdrawalDTO getPendingWithdrawalDTO() {
    return pendingWithdrawalDTO;
  }

  public void setPendingWithdrawalDTO(PendingWithdrawalDTO pendingWithdrawalDTO) {
    this.pendingWithdrawalDTO = pendingWithdrawalDTO;
  }

  public String getCustomerAddress() {
    return customerAddress;
  }

  public void setCustomerAddress(String customerAddress) {
    this.customerAddress = customerAddress;
  }

  public String getStateReason() {
    return stateReason;
  }

  public void setStateReason(String stateReason) {
    this.stateReason = stateReason;
  }

  public TransactionWithdrawalStateEnum getState() {
    return state;
  }

  public void setState(TransactionWithdrawalStateEnum state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
