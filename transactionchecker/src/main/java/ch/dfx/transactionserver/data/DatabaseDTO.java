package ch.dfx.transactionserver.data;

import org.apache.commons.codec.digest.DigestUtils;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public abstract class DatabaseDTO {

  // ...
  private transient String internalStateHolder = null;

  /**
   * 
   */
  public DatabaseDTO() {
  }

  /**
   * 
   */
  public void keepInternalState() {
    internalStateHolder = DigestUtils.md5Hex(this.toString());
  }

  /**
   * 
   */
  public boolean isInternalStateChanged() {
    return !DigestUtils.md5Hex(this.toString()).equals(internalStateHolder);
  }

  /**
   * 
   */
  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
