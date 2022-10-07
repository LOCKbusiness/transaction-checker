package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class AddressTransactionOutDTO {
  private final Integer blockNumber;
  private final Integer transactionNumber;
  private final Integer voutNumber;
  private final Integer addressNumber;

  private BigDecimal vout = null;
  private String type = null;

  /**
   * 
   */
  public AddressTransactionOutDTO(
      @Nonnull Integer blockNumber,
      @Nonnull Integer transactionNumber,
      @Nonnull Integer voutNumber,
      @Nonnull Integer addressNumber) {
    this.blockNumber = blockNumber;
    this.transactionNumber = transactionNumber;
    this.voutNumber = voutNumber;
    this.addressNumber = addressNumber;
  }

  public Integer getBlockNumber() {
    return blockNumber;
  }

  public Integer getTransactionNumber() {
    return transactionNumber;
  }

  public Integer getVoutNumber() {
    return voutNumber;
  }

  public Integer getAddressNumber() {
    return addressNumber;
  }

  public BigDecimal getVout() {
    return vout;
  }

  public void setVout(BigDecimal vout) {
    this.vout = vout;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
