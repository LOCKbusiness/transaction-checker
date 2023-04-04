package ch.dfx.common.errorhandling;

import javax.annotation.Nonnull;

import ch.dfx.defichain.data.ResultErrorData;

/**
 * 
 */
public class DefiChainException extends DfxException {
  private static final long serialVersionUID = 439767929692766714L;

  private final ResultErrorData resultErrorData;

  /**
   * 
   */
  public DefiChainException(@Nonnull ResultErrorData resultErrorData) {
    super("[" + resultErrorData.getCode() + "] " + resultErrorData.getMessage());

    this.resultErrorData = resultErrorData;
  }

  /**
   * 
   */
  public ResultErrorData getResultErrorData() {
    return resultErrorData;
  }
}
