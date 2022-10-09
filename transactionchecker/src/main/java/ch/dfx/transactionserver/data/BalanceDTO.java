package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class BalanceDTO {
  private final int addressNumber;
  private int blockNumber = -1;
  private int transactionCount = 0;

  private BigDecimal vout = BigDecimal.ZERO;
  private BigDecimal vin = BigDecimal.ZERO;

  /**
   * 
   */
  public BalanceDTO(int addressNumber) {
    this.addressNumber = addressNumber;
  }

  public int getAddressNumber() {
    return addressNumber;
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
  }

  public int getTransactionCount() {
    return transactionCount;
  }

  public void setTransactionCount(int transactionCount) {
    this.transactionCount = transactionCount;
  }

  public void addTransactionCount(int transactionCount) {
    this.transactionCount += transactionCount;
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

  public BigDecimal getVin() {
    return vin;
  }

  public void setVin(BigDecimal vin) {
    this.vin = vin;
  }

  public void addVin(@Nonnull BigDecimal vin) {
    this.vin = this.vin.add(vin);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
