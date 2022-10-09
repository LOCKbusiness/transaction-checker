package ch.dfx.ocean.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionsPageDTO {
  private String next = null;

  /**
   * 
   */
  public TransactionsPageDTO() {
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
