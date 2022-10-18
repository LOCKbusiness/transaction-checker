package ch.dfx.api.data.transaction;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class OpenTransactionPayloadDTO {
  private Integer id = null;

  /**
   * 
   */
  public OpenTransactionPayloadDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
