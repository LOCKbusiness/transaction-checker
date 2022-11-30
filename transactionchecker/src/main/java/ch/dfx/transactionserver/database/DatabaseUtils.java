package ch.dfx.transactionserver.database;

import java.sql.Connection;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 */
public class DatabaseUtils {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseUtils.class);

  /**
   * 
   */
  public static void rollback(@Nullable Connection connection) {
    LOGGER.trace("rollback()");

    try {
      if (null != connection) {
        connection.rollback();
      }
    } catch (Exception e) {
      LOGGER.error("rollback", e);
    }
  }

  /**
   * 
   */
  private DatabaseUtils() {
  }
}
