package ch.dfx.transactionserver.data;

/**
 * 
 */
public class LiquidityDTO extends DatabaseDTO {
  private int addressNumber = -1;
  private int startBlockNumber = -1;
  private int startTransactionNumber = -1;

  private String address = null;

  /**
   * 
   */
  public LiquidityDTO() {
  }

  public int getAddressNumber() {
    return addressNumber;
  }

  public void setAddressNumber(int addressNumber) {
    this.addressNumber = addressNumber;
  }

  public int getStartBlockNumber() {
    return startBlockNumber;
  }

  public void setStartBlockNumber(int startBlockNumber) {
    this.startBlockNumber = startBlockNumber;
  }

  public int getStartTransactionNumber() {
    return startTransactionNumber;
  }

  public void setStartTransactionNumber(int startTransactionNumber) {
    this.startTransactionNumber = startTransactionNumber;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
