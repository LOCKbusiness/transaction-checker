package ch.dfx.transactionserver.data;

import java.math.BigDecimal;

/**
 * 
 */
public class TransactionCustomAccountToAccountInDTO extends DatabaseDTO {
  private final int blockNumber;
  private final int transactionNumber;
  private final int typeNumber;
  private final int addressNumber;
  private final int tokenNumber;

  private BigDecimal amount = null;

  /**
   * 
   */
  public TransactionCustomAccountToAccountInDTO(
      int blockNumber,
      int transactionNumber,
      int typeNumber,
      int addressNumber,
      int tokenNumber) {
    this.blockNumber = blockNumber;
    this.transactionNumber = transactionNumber;
    this.typeNumber = typeNumber;
    this.addressNumber = addressNumber;
    this.tokenNumber = tokenNumber;
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public int getTransactionNumber() {
    return transactionNumber;
  }

  public int getTypeNumber() {
    return typeNumber;
  }

  public int getAddressNumber() {
    return addressNumber;
  }

  public Integer getTokenNumber() {
    return tokenNumber;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
