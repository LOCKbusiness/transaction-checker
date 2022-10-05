package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class CacheAddressTransactionInData {
  private final Integer blockNumber;
  private final Integer transactionNumber;
  private final Integer vinNumber;
  private final Integer addressNumber;

  private final Integer inBlockNumber;
  private final Integer inTransactionNumber;

  private BigDecimal vin = null;

  /**
   * 
   */
  public CacheAddressTransactionInData(
      @Nonnull Integer blockNumber,
      @Nonnull Integer transactionNumber,
      @Nonnull Integer vinNumber,
      @Nonnull Integer addressNumber,
      @Nonnull Integer inBlockNumber,
      @Nonnull Integer inTransactionNumber) {
    this.blockNumber = blockNumber;
    this.transactionNumber = transactionNumber;
    this.vinNumber = vinNumber;
    this.addressNumber = addressNumber;
    this.inBlockNumber = inBlockNumber;
    this.inTransactionNumber = inTransactionNumber;
  }

  public Integer getBlockNumber() {
    return blockNumber;
  }

  public Integer getTransactionNumber() {
    return transactionNumber;
  }

  public Integer getVinNumber() {
    return vinNumber;
  }

  public Integer getAddressNumber() {
    return addressNumber;
  }

  public Integer getInBlockNumber() {
    return inBlockNumber;
  }

  public Integer getInTransactionNumber() {
    return inTransactionNumber;
  }

  public BigDecimal getVin() {
    return vin;
  }

  public void setVin(BigDecimal vin) {
    this.vin = vin;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
