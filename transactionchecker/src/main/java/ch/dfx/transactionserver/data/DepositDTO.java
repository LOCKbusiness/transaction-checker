package ch.dfx.transactionserver.data;

import javax.annotation.Nullable;

/**
 * 
 */
public class DepositDTO extends DatabaseDTO {
  private int liquidityAddressNumber = -1;
  private int depositAddressNumber = -1;
  private int customerAddressNumber = -1;
  private int startBlockNumber = -1;
  private int startTransactionNumber = -1;

  private String liquidityAddress = null;
  private String depositAddress = null;
  private String customerAddress = null;

  /**
   * 
   */
  public DepositDTO() {
  }

  public int getLiquidityAddressNumber() {
    return liquidityAddressNumber;
  }

  public void setLiquidityAddressNumber(int liquidityAddressNumber) {
    this.liquidityAddressNumber = liquidityAddressNumber;
  }

  public int getDepositAddressNumber() {
    return depositAddressNumber;
  }

  public void setDepositAddressNumber(int depositAddressNumber) {
    this.depositAddressNumber = depositAddressNumber;
  }

  public int getCustomerAddressNumber() {
    return customerAddressNumber;
  }

  public void setCustomerAddressNumber(int customerAddressNumber) {
    this.customerAddressNumber = customerAddressNumber;
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

  public String getLiquidityAddress() {
    return liquidityAddress;
  }

  public void setLiquidityAddress(String liquidityAddress) {
    this.liquidityAddress = liquidityAddress;
  }

  public @Nullable String getDepositAddress() {
    return depositAddress;
  }

  public void setDepositAddress(@Nullable String depositAddress) {
    this.depositAddress = depositAddress;
  }

  public @Nullable String getCustomerAddress() {
    return customerAddress;
  }

  public void setCustomerAddress(@Nullable String customerAddress) {
    this.customerAddress = customerAddress;
  }
}
