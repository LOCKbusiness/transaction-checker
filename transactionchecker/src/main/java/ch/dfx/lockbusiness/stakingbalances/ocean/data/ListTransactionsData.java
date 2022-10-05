package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import java.util.List;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class ListTransactionsData {
  private List<TransactionsData> datalist = null;

  /**
   * 
   */
  public ListTransactionsData() {
  }

  public List<TransactionsData> getDatalist() {
    return datalist;
  }

  public void setDatalist(List<TransactionsData> datalist) {
    this.datalist = datalist;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
