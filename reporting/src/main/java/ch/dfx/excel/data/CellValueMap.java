package ch.dfx.excel.data;

import java.util.HashMap;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class CellValueMap extends HashMap<Integer, Object> {
  private static final long serialVersionUID = -8595002963670625305L;

  /**
   * 
   */
  public CellValueMap() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
