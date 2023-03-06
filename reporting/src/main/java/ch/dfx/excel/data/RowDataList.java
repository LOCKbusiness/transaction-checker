package ch.dfx.excel.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class RowDataList extends ArrayList<RowData> {
  private static final long serialVersionUID = 4941141532109209586L;

  private final int rowOffset;

  private final Map<String, Object> propertyMap;

  /**
   * 
   */
  public RowDataList(int rowOffset) {
    this.rowOffset = rowOffset;

    this.propertyMap = new HashMap<>();
  }

  public int getRowOffset() {
    return rowOffset;
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
