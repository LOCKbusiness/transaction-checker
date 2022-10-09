package ch.dfx.transactionserver.data;

import javax.annotation.Nullable;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DepositDTO {
  private int customerAddressNumber = -1;
  private int depositAddressNumber = -1;
  private int startBlockNumber = -1;
  private int startTransactionNumber = -1;

  private String customerAddress = null;
  private String depositAddress = null;

  /**
   * 
   */
  public DepositDTO() {
  }

  public int getCustomerAddressNumber() {
    return customerAddressNumber;
  }

  public void setCustomerAddressNumber(int customerAddressNumber) {
    this.customerAddressNumber = customerAddressNumber;
  }

  public int getDepositAddressNumber() {
    return depositAddressNumber;
  }

  public void setDepositAddressNumber(int depositAddressNumber) {
    this.depositAddressNumber = depositAddressNumber;
  }

  public int getStartBlockNumber() {
    return startBlockNumber;
  }

  public void setStartBlockNumber(int startBlockNumber) {
    this.startBlockNumber = startBlockNumber;
  }

  public int getStartTransactionNumber() {
    return startTransactionNumber;
  }

  public void setStartTransactionNumber(int startTransactionNumber) {
    this.startTransactionNumber = startTransactionNumber;
  }

  public @Nullable String getCustomerAddress() {
    return customerAddress;
  }

  public void setCustomerAddress(@Nullable String customerAddress) {
    this.customerAddress = customerAddress;
  }

  public @Nullable String getDepositAddress() {
    return depositAddress;
  }

  public void setDepositAddress(@Nullable String depositAddress) {
    this.depositAddress = depositAddress;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
