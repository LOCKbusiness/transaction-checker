package ch.dfx.graphgl.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class FarmingHistoryPoolDTOList extends ArrayList<FarmingHistoryPoolDTO> {
  private static final long serialVersionUID = -2322301106750890489L;

  /**
   * 
   */
  public FarmingHistoryPoolDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
