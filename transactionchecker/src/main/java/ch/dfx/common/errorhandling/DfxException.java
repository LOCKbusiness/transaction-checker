package ch.dfx.common.errorhandling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 */
public class DfxException extends Exception {
  private static final long serialVersionUID = -6566475107343849995L;

  private static final Logger LOGGER = LogManager.getLogger(DfxException.class);

  /**
   * 
   */
  public DfxException(String errorMessage) {
    super("[ERROR] " + errorMessage);

    LOGGER.error(errorMessage);
  }

  /**
   * 
   */
  public DfxException(String errorMessage, Throwable throwable) {
    super("[ERROR] " + errorMessage, throwable);

    LOGGER.error(errorMessage, throwable);
  }
}
