package ch.dfx.transactionserver.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class CacheBlockData {
  private final Integer number;
  private final String hash;

  private final List<CacheTransactionData> transactionDataList;

  /**
   * 
   */
  public CacheBlockData(
      @Nonnull Integer number,
      @Nonnull String hash) {
    this.number = number;
    this.hash = hash;

    this.transactionDataList = new ArrayList<>();
  }

  public Integer getNumber() {
    return number;
  }

  public String getHash() {
    return hash;
  }

  public List<CacheTransactionData> getTransactionDataList() {
    return transactionDataList;
  }

  /**
   * 
   */
  public void addTransaction(@Nonnull CacheTransactionData transactionData) {
    this.transactionDataList.add(transactionData);
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
