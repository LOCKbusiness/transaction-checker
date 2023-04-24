package ch.dfx.ocean.data;

import java.util.List;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryDTO {
  private List<HistoryDetailDTO> data;

  private NextPageDTO page = null;

  /**
   * 
   */
  public HistoryDTO() {
  }

  public List<HistoryDetailDTO> getData() {
    return data;
  }

  public void setData(List<HistoryDetailDTO> data) {
    this.data = data;
  }

  public NextPageDTO getPage() {
    return page;
  }

  public void setPage(NextPageDTO page) {
    this.page = page;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
