package ch.dfx.transactionserver.data;

import javax.annotation.Nonnull;

/**
 * 
 */
public class BlockTransactionDTO extends DatabaseDTO {
  private final Integer blockNumber;
  private final String blockHash;

  private final Integer transactionNumber;
  private final String transactionId;

  /**
   * 
   */
  public BlockTransactionDTO(
      @Nonnull Integer blockNumber,
      @Nonnull String blockHash,
      @Nonnull Integer transactionNumber,
      @Nonnull String transactionId) {
    this.blockNumber = blockNumber;
    this.blockHash = blockHash;
    this.transactionNumber = transactionNumber;
    this.transactionId = transactionId;
  }

  public Integer getBlockNumber() {
    return blockNumber;
  }

  public String getBlockHash() {
    return blockHash;
  }

  public Integer getTransactionNumber() {
    return transactionNumber;
  }

  public String getTransactionId() {
    return transactionId;
  }
}
