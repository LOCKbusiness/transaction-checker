package ch.dfx.transactionserver.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class BlockDTO {
  private final Integer number;
  private final String hash;

  private final List<TransactionDTO> transactionDTOList;

  /**
   * 
   */
  public BlockDTO(
      @Nonnull Integer number,
      @Nonnull String hash) {
    this.number = number;
    this.hash = hash;

    this.transactionDTOList = new ArrayList<>();
  }

  public Integer getNumber() {
    return number;
  }

  public String getHash() {
    return hash;
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

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
