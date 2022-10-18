package ch.dfx.api.data.join;

import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class WithdrawalTransactionDTO {
  private Integer id = null;

  private PendingWithdrawalDTO pendingWithdrawalDTO = null;
  private OpenTransactionDTO openTransactionDTO = null;

  // ...
  private String address = null;

  private String checkMessage = null;
  private WithdrawalTransactionStateEnum state = WithdrawalTransactionStateEnum.OPEN;

  /**
   * 
   */
  public WithdrawalTransactionDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public PendingWithdrawalDTO getPendingWithdrawalDTO() {
    return pendingWithdrawalDTO;
  }

  public void setPendingWithdrawalDTO(PendingWithdrawalDTO pendingWithdrawalDTO) {
    this.pendingWithdrawalDTO = pendingWithdrawalDTO;
  }

  public OpenTransactionDTO getOpenTransactionDTO() {
    return openTransactionDTO;
  }

  public void setOpenTransactionDTO(OpenTransactionDTO openTransactionDTO) {
    this.openTransactionDTO = openTransactionDTO;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCheckMessage() {
    return checkMessage;
  }

  public void setCheckMessage(String checkMessage) {
    this.checkMessage = checkMessage;
  }

  public WithdrawalTransactionStateEnum getState() {
    return state;
  }

  public void setState(WithdrawalTransactionStateEnum state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
