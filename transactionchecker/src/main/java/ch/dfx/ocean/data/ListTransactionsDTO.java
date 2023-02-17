package ch.dfx.ocean.data;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class ListTransactionsDTO {
  private List<TransactionsDTO> datalist = null;

  /**
   * 
   */
  public ListTransactionsDTO() {
  }

  public List<TransactionsDTO> getDatalist() {
    return datalist;
  }

  public void setDatalist(List<TransactionsDTO> datalist) {
    this.datalist = datalist;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
