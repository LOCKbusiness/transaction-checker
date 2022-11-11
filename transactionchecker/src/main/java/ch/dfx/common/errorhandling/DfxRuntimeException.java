package ch.dfx.common.errorhandling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 */
public class DfxRuntimeException extends RuntimeException {
  private static final long serialVersionUID = -5600037736780167648L;

  private static final Logger LOGGER = LogManager.getLogger(DfxRuntimeException.class);

  /**
   * 
   */
  public DfxRuntimeException(String errorMessage) {
    super("[ERROR] " + errorMessage);

    LOGGER.error(errorMessage);
  }

  /**
   * 
   */
  public DfxRuntimeException(String errorMessage, Throwable throwable) {
    super("[ERROR] " + errorMessage, throwable);

    LOGGER.error(errorMessage, throwable);
  }
}
