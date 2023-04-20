package ch.dfx.reporting.transparency.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public abstract class HistorySheetDTO {
  private String timestamp = null;

  /**
   * 
   */
  public HistorySheetDTO() {
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
