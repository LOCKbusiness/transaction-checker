package ch.dfx.transactionserver.importer.data;

import java.util.ArrayList;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class DataImporterMasternodeOwnerDataList extends ArrayList<DataImporterMasternodeOwnerData> {
  private static final long serialVersionUID = 8194562103460224954L;

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
