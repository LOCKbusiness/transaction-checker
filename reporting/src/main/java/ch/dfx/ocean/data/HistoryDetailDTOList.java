package ch.dfx.ocean.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

public class HistoryDetailDTOList extends ArrayList<HistoryDetailDTO> {
  private static final long serialVersionUID = -3806208572005800961L;

  /**
   * 
   */
  public HistoryDetailDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
