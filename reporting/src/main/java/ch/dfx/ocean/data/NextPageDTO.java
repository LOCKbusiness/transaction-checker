package ch.dfx.ocean.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class NextPageDTO {
  private String next = null;

  /**
   * 
   */
  public NextPageDTO() {
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
