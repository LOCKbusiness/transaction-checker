package ch.dfx.graphgl.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class FarmingHistoryGetDTOList extends ArrayList<FarmingHistoryGetDTO> {
  private static final long serialVersionUID = -9211482792156380213L;

  /**
   * 
   */
  public FarmingHistoryGetDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
