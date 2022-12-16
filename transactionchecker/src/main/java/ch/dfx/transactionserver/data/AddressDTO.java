package ch.dfx.transactionserver.data;

import javax.annotation.Nonnull;

/**
 * 
 */
public class AddressDTO extends DatabaseDTO {
  private final Integer number;
  private final String address;

  /**
   * 
   */
  public AddressDTO(
      @Nonnull Integer number,
      @Nonnull String address) {
    this.number = number;
    this.address = address;
  }

  public Integer getNumber() {
    return number;
  }

  public String getAddress() {
    return address;
  }
}
