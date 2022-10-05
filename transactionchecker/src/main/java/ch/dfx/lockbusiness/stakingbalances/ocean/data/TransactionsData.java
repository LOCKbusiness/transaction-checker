package ch.dfx.lockbusiness.stakingbalances.ocean.data;

import java.util.List;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class TransactionsData {
  private List<TransactionsDetailData> data;

  private TransactionsPageData page = null;

  /**
   * 
   */
  public TransactionsData() {
  }

  public List<TransactionsDetailData> getData() {
    return data;
  }

  public void setData(List<TransactionsDetailData> data) {
    this.data = data;
  }

  public TransactionsPageData getPage() {
    return page;
  }

  public void setPage(TransactionsPageData page) {
    this.page = page;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
