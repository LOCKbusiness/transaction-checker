package ch.dfx.transactionserver.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 
 */
public class BlockDTO extends DatabaseDTO {
  private final Integer number;
  private final String hash;
  private final Long timestamp;

  private final List<TransactionDTO> transactionDTOList;

  /**
   * 
   */
  public BlockDTO(
      @Nonnull Integer number,
      @Nonnull String hash,
      @Nonnull Long timestamp) {
    this.number = number;
    this.hash = hash;
    this.timestamp = timestamp;

    this.transactionDTOList = new ArrayList<>();
  }

  public Integer getNumber() {
    return number;
  }

  public String getHash() {
    return hash;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public List<TransactionDTO> getTransactionDTOList() {
    return transactionDTOList;
  }

  /**
   * 
   */
  public void addTransactionDTO(@Nonnull TransactionDTO transactionDTO) {
    this.transactionDTOList.add(transactionDTO);
  }
}
