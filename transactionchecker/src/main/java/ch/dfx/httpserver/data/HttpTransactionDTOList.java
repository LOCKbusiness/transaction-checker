package ch.dfx.httpserver.data;

import java.util.ArrayList;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class HttpTransactionDTOList extends ArrayList<HttpTransactionDTO> {
  private static final long serialVersionUID = 6623566451024443473L;

  /**
   * 
   */
  public HttpTransactionDTOList() {
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
