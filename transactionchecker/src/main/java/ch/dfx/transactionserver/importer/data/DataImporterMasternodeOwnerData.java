package ch.dfx.transactionserver.importer.data;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class DataImporterMasternodeOwnerData {
  private String wallet;
  private int index;
  private String address;

  /**
   * 
   */
  public DataImporterMasternodeOwnerData() {
  }

  public String getWallet() {
    return wallet;
  }

  public void setWallet(String wallet) {
    this.wallet = wallet;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
