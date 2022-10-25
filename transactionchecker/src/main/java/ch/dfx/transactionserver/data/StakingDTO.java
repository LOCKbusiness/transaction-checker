package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class StakingDTO {
  private final int liquidityAddressNumber;
  private final int depositAddressNumber;
  private final int customerAddressNumber;

  private int lastInBlockNumber = -1;
  private BigDecimal vin = BigDecimal.ZERO;

  private int lastOutBlockNumber = -1;
  private BigDecimal vout = BigDecimal.ZERO;

  private DepositDTO depositDTO = null;

  private String liquidityAddress = null;
  private String depositAddress = null;
  private String customerAddress = null;

  /**
   * 
   */
  public StakingDTO(
      int liquidityAddressNumber,
      int depositAddressNumber,
      int customerAddressNumber) {
    this.liquidityAddressNumber = liquidityAddressNumber;
    this.depositAddressNumber = depositAddressNumber;
    this.customerAddressNumber = customerAddressNumber;
  }

  public int getLiquidityAddressNumber() {
    return liquidityAddressNumber;
  }

  public int getDepositAddressNumber() {
    return depositAddressNumber;
  }

  public int getCustomerAddressNumber() {
    return customerAddressNumber;
  }

  public int getLastInBlockNumber() {
    return lastInBlockNumber;
  }

  public void setLastInBlockNumber(int lastInBlockNumber) {
    this.lastInBlockNumber = lastInBlockNumber;
  }

  public BigDecimal getVin() {
    return vin;
  }

  public void setVin(BigDecimal vin) {
    this.vin = vin;
  }

  public void addVin(@Nonnull BigDecimal vin) {
    this.vin = this.vin.add(vin);
  }

  public int getLastOutBlockNumber() {
    return lastOutBlockNumber;
  }

  public void setLastOutBlockNumber(int lastOutBlockNumber) {
    this.lastOutBlockNumber = lastOutBlockNumber;
  }

  public BigDecimal getVout() {
    return vout;
  }

  public void setVout(BigDecimal vout) {
    this.vout = vout;
  }

  public void addVout(@Nonnull BigDecimal vout) {
    this.vout = this.vout.add(vout);
  }

  public @Nullable DepositDTO getDepositDTO() {
    return depositDTO;
  }

  public void setDepositDTO(@Nullable DepositDTO depositDTO) {
    this.depositDTO = depositDTO;
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

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
