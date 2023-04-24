package ch.dfx.ocean.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class HistoryBlockDTO {
  private long height = -1;
  private String hash = null;
  private long time = -1;

  /**
   * 
   */
  public HistoryBlockDTO() {
  }

  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
