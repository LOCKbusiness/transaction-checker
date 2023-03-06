package ch.dfx.excel.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class CellDataList extends ArrayList<CellData> {
  private static final long serialVersionUID = -5128911485588295763L;

  private final Map<String, Object> propertyMap;

  /**
   * 
   */
  public CellDataList() {
    this.propertyMap = new HashMap<>();
  }

  public void addProperty(
      @Nonnull String propertyName,
      @Nonnull Object propertyValue) {
    propertyMap.put(propertyName, propertyValue);
  }

  public void removeProperty(@Nonnull String propertyName) {
    propertyMap.remove(propertyName);
  }

  public Object getProperty(@Nonnull String propertyName) {
    return propertyMap.get(propertyName);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
