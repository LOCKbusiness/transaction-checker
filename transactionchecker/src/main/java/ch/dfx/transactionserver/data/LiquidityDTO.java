package ch.dfx.transactionserver.data;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class LiquidityDTO {
  private int addressNumber = -1;
  private int startBlockNumber = -1;
  private int startTransactionNumber = -1;

  /**
   * 
   */
  public LiquidityDTO() {
  }

  public int getAddressNumber() {
    return addressNumber;
  }

  public void setAddressNumber(int addressNumber) {
    this.addressNumber = addressNumber;
  }

  public int getStartBlockNumber() {
    return startBlockNumber;
  }

  public void setStartBlockNumber(int startBlockNumber) {
    this.startBlockNumber = startBlockNumber;
  }

  public int getStartTransactionNumber() {
    return startTransactionNumber;
  }

  public void setStartTransactionNumber(int startTransactionNumber) {
    this.startTransactionNumber = startTransactionNumber;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
