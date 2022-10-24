package ch.dfx.transactionserver.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class DatabaseUtils {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseUtils.class);

  /**
   * 
   */
  public static void rollback(@Nullable Connection connection) {
    LOGGER.trace("rollback() ...");

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
  public static int getIntOrDefault(
      @Nonnull ResultSet resultSet,
      @Nonnull String columnLabel,
      int defaultValue) throws DfxException {
    try {
      int value = resultSet.getInt(columnLabel);

      if (resultSet.wasNull()) {
        value = defaultValue;
      }

      return value;
    } catch (Exception e) {
      throw new DfxException("getIntOrDefault", e);
    }
  }

  /**
   * 
   */
  public static BigDecimal getBigDecimalOrDefault(
      @Nonnull ResultSet resultSet,
      @Nonnull String columnLabel,
      BigDecimal defaultValue) throws DfxException {
    try {
      BigDecimal value = resultSet.getBigDecimal(columnLabel);

      if (resultSet.wasNull()) {
        value = defaultValue;
      }

      return value;
    } catch (Exception e) {
      throw new DfxException("getBigDecimalOrDefault", e);
    }
  }

  /**
   * 
   */
  private DatabaseUtils() {
  }
}
