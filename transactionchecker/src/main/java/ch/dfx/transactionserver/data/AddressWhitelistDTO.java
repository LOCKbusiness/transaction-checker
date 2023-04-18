package ch.dfx.transactionserver.data;

import javax.annotation.Nonnull;

/**
 * 
 */
public class AddressWhitelistDTO extends DatabaseDTO {
  private final String type;
  private final String address;

  private String remark = null;

  /**
   * 
   */
  public AddressWhitelistDTO(
      @Nonnull String type,
      @Nonnull String address) {
    this.type = type;
    this.address = address;
  }

  public String getType() {
    return type;
  }

  public String getAddress() {
    return address;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
