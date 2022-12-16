package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

/**
 * 
 */
public class AddressTransactionOutDTO extends DatabaseDTO {
  private final int blockNumber;
  private final int transactionNumber;
  private final int voutNumber;
  private final int addressNumber;

  private BigDecimal vout = null;
  private String type = null;

  /**
   * 
   */
  public AddressTransactionOutDTO(
      int blockNumber,
      int transactionNumber,
      int voutNumber,
      int addressNumber) {
    this.blockNumber = blockNumber;
    this.transactionNumber = transactionNumber;
    this.voutNumber = voutNumber;
    this.addressNumber = addressNumber;
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public int getTransactionNumber() {
    return transactionNumber;
  }

  public int getVoutNumber() {
    return voutNumber;
  }

  public int getAddressNumber() {
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
}
