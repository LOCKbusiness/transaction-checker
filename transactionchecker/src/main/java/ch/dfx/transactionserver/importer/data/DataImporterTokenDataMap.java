package ch.dfx.transactionserver.importer.data;

import java.util.LinkedHashMap;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class DataImporterTokenDataMap extends LinkedHashMap<String, DataImporterTokenData> {
  private static final long serialVersionUID = 7314098623711710448L;

  /**
   * 
   */
  public DataImporterTokenDataMap() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
