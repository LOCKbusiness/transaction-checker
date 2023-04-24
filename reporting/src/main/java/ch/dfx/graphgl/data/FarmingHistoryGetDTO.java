package ch.dfx.graphgl.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class FarmingHistoryGetDTO {
  private FarmingHistoryPoolDTOList pools = null;

  /**
   * 
   */
  public FarmingHistoryGetDTO() {
  }

  public FarmingHistoryPoolDTOList getPools() {
    return pools;
  }

  public void setPools(FarmingHistoryPoolDTOList pools) {
    this.pools = pools;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
