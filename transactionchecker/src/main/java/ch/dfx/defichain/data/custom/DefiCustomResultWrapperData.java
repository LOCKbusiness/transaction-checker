package ch.dfx.defichain.data.custom;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.data.ResultDataA;

/**
 * 
 */
public class DefiCustomResultWrapperData extends ResultDataA {
  /**
   * 
   */
  public DefiCustomResultWrapperData() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
