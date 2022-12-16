package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

/**
 * 
 */
public class AddressTransactionInDTO extends DatabaseDTO {
  private final int blockNumber;
  private final int transactionNumber;
  private final int vinNumber;
  private final int addressNumber;

  private final int inBlockNumber;
  private final int inTransactionNumber;

  private BigDecimal vin = null;

  /**
   * 
   */
  public AddressTransactionInDTO(
      int blockNumber,
      int transactionNumber,
      int vinNumber,
      int addressNumber,
      int inBlockNumber,
      int inTransactionNumber) {
    this.blockNumber = blockNumber;
    this.transactionNumber = transactionNumber;
    this.vinNumber = vinNumber;
    this.addressNumber = addressNumber;
    this.inBlockNumber = inBlockNumber;
    this.inTransactionNumber = inTransactionNumber;
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public int getTransactionNumber() {
    return transactionNumber;
  }

  public int getVinNumber() {
    return vinNumber;
  }

  public int getAddressNumber() {
    return addressNumber;
  }

  public int getInBlockNumber() {
    return inBlockNumber;
  }

  public int getInTransactionNumber() {
    return inTransactionNumber;
  }

  public BigDecimal getVin() {
    return vin;
  }

  public void setVin(BigDecimal vin) {
    this.vin = vin;
  }
}
