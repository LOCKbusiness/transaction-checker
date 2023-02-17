package ch.dfx.transactionserver.importer.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class DataImporterTokenData {
  private String symbol = null;
  private String symbolKey = null;
  private String name = null;

  private boolean isDAT = false;
  private boolean isLPS = false;

  /**
   * 
   */
  public DataImporterTokenData() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getSymbolKey() {
    return symbolKey;
  }

  public void setSymbolKey(String symbolKey) {
    this.symbolKey = symbolKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isDAT() {
    return isDAT;
  }

  public void setDAT(boolean isDAT) {
    this.isDAT = isDAT;
  }

  public boolean isLPS() {
    return isLPS;
  }

  public void setLPS(boolean isLPS) {
    this.isLPS = isLPS;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
