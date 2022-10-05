package ch.dfx.transactionserver.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class CacheTransactionData {
  private final Integer blockNumber;

  private final Integer number;
  private final String transactionId;

  private final List<CacheAddressTransactionOutData> addressTransactionOutDataList;
  private final List<CacheAddressTransactionInData> addressTransactionInDataList;

  /**
   * 
   */
  public CacheTransactionData(
      @Nonnull Integer blockNumber,
      @Nonnull Integer number,
      @Nonnull String transactionId) {
    this.blockNumber = blockNumber;
    this.number = number;
    this.transactionId = transactionId;

    this.addressTransactionOutDataList = new ArrayList<>();
    this.addressTransactionInDataList = new ArrayList<>();
  }

  public Integer getBlockNumber() {
    return blockNumber;
  }

  public Integer getNumber() {
    return number;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public List<CacheAddressTransactionOutData> getAddressTransactionDataOutList() {
    return addressTransactionOutDataList;
  }

  /**
   * 
   */
  public void addAddressTransactionOutData(CacheAddressTransactionOutData addressTransactionOutData) {
    this.addressTransactionOutDataList.add(addressTransactionOutData);
  }

  public List<CacheAddressTransactionInData> getAddressTransactionDataInList() {
    return addressTransactionInDataList;
  }

  /**
   * 
   */
  public void addAddressTransactionInData(CacheAddressTransactionInData addressTransactionInData) {
    this.addressTransactionInDataList.add(addressTransactionInData);
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
