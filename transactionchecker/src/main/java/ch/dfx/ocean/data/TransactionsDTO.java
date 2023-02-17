package ch.dfx.ocean.data;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class TransactionsDTO {
  private List<TransactionsDetailDTO> data;

  private TransactionsPageDTO page = null;

  /**
   * 
   */
  public TransactionsDTO() {
  }

  public List<TransactionsDetailDTO> getData() {
    return data;
  }

  public void setData(List<TransactionsDetailDTO> data) {
    this.data = data;
  }

  public TransactionsPageDTO getPage() {
    return page;
  }

  public void setPage(TransactionsPageDTO page) {
    this.page = page;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
