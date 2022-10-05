package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class TransactionsPageData {
  private String next = null;

  /**
   * 
   */
  public TransactionsPageData() {
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
