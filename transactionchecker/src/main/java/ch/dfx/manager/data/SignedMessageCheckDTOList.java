package ch.dfx.manager.data;

import java.util.ArrayList;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class SignedMessageCheckDTOList extends ArrayList<SignedMessageCheckDTO> {
  private static final long serialVersionUID = 3911221784127212087L;

  /**
   * 
   */
  public SignedMessageCheckDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
