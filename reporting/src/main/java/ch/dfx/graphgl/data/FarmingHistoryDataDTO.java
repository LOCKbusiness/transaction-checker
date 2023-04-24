package ch.dfx.graphgl.data;

import java.util.Map;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class FarmingHistoryDataDTO {
  private Map<String, FarmingHistoryGetDTOList> data = null;

  /**
   * 
   */
  public FarmingHistoryDataDTO() {
  }

  public Map<String, FarmingHistoryGetDTOList> getData() {
    return data;
  }

  public void setData(Map<String, FarmingHistoryGetDTOList> data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
