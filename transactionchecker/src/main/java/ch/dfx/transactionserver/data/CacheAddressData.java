package ch.dfx.transactionserver.data;

import javax.annotation.Nonnull;

import ch.dfx.common.PayoutManagerUtils;

/**
 * 
 */
public class CacheAddressData {
  private final Integer number;
  private final String address;
  private final String hex;

  /**
   * 
   */
  public CacheAddressData(
      @Nonnull Integer number,
      @Nonnull String address,
      @Nonnull String hex) {
    this.number = number;
    this.address = address;
    this.hex = hex;
  }

  public Integer getNumber() {
    return number;
  }

  public String getAddress() {
    return address;
  }

  public String getHex() {
    return hex;
  }

  @Override
  public String toString() {
    return PayoutManagerUtils.toJson(this);
  }
}
