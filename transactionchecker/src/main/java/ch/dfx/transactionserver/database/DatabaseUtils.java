package ch.dfx.transactionserver.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class DatabaseUtils {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseUtils.class);

  public static final String TOKEN_PUBLIC_SCHEMA = "${public_schema}";
  public static final String TOKEN_NETWORK_SCHEMA = "${network_schema}";
  public static final String TOKEN_NETWORK_CUSTOM_SCHEMA = "${network_custom_schema}";

  public static final String TOKEN_STAKING_SCHEMA = "${staking_schema}";
  public static final String TOKEN_YIELDMACHINE_SCHEMA = "${yieldmachine_schema}";

  /**
   * 
   */
  public static String replaceSchema(
      @Nonnull NetworkEnum network,
      @Nonnull String sql) {
    String schemaReplacedSql = replacePublicSchema(sql);
    schemaReplacedSql = replaceNetworkSchema(network, schemaReplacedSql);
    schemaReplacedSql = replaceNetworkCustomSchema(network, schemaReplacedSql);
    schemaReplacedSql = replaceStakingSchema(network, schemaReplacedSql);
    schemaReplacedSql = replaceYieldmachineSchema(network, schemaReplacedSql);

    return schemaReplacedSql;
  }

  /**
   * 
   */
  private static String replacePublicSchema(@Nonnull String sql) {
    return sql.replace(TOKEN_PUBLIC_SCHEMA, "public");
  }

  /**
   * 
   */
  private static String replaceNetworkSchema(
      @Nonnull NetworkEnum network,
      @Nonnull String sql) {
    return sql.replace(TOKEN_NETWORK_SCHEMA, network.toString());
  }

  /**
   * 
   */
  private static String replaceNetworkCustomSchema(
      @Nonnull NetworkEnum network,
      @Nonnull String sql) {
    return sql.replace(TOKEN_NETWORK_CUSTOM_SCHEMA, network.toString() + "_custom");
  }

  /**
   * 
   */
  private static String replaceStakingSchema(
      @Nonnull NetworkEnum network,
      @Nonnull String sql) {
    return sql.replace(TOKEN_STAKING_SCHEMA, network.toString() + "_staking");
  }

  /**
   * 
   */
  private static String replaceYieldmachineSchema(
      @Nonnull NetworkEnum network,
      @Nonnull String sql) {
    return sql.replace(TOKEN_YIELDMACHINE_SCHEMA, network.toString() + "_yieldmachine");
  }

  /**
   * 
   */
  public synchronized static int getNextBlockNumber(
      @Nonnull NetworkEnum network,
      @Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getNextBlockNumber()");

    String sqlSelect =
        new StringBuilder()
            .append(replaceSchema(network, "SELECT MAX(number) FROM " + TOKEN_PUBLIC_SCHEMA + ".block"))
            .toString();

    return getNextNumber(connection, sqlSelect);
  }

  /**
   * 
   */
  public synchronized static int getNextAddressNumber(
      @Nonnull NetworkEnum network,
      @Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getNextAddressNumber()");

    String sqlSelect =
        new StringBuilder()
            .append(replaceSchema(network, "SELECT MAX(number) FROM " + TOKEN_PUBLIC_SCHEMA + ".address"))
            .toString();

    return getNextNumber(connection, sqlSelect);
  }

  /**
   * 
   */
  public synchronized static int getNextNumber(
      @Nonnull Connection connection,
      @Nonnull String sqlSelect) throws DfxException {
    LOGGER.trace("getNextNumber(): sqlSelect = " + sqlSelect);

    try (Statement statement = connection.createStatement()) {
      int nextNumber = -1;

      ResultSet resultSet = statement.executeQuery(sqlSelect);

      if (resultSet.next()) {
        nextNumber = resultSet.getInt(1);

        if (!resultSet.wasNull()) {
          nextNumber++;
        }
      }

      resultSet.close();

      if (-1 == nextNumber) {
        throw new DfxException("next number not found ...");
      }

      return nextNumber;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getNextNumber", e);
    }
  }

  /**
   * 
   */
  public synchronized static void rollback(@Nullable Connection connection) {
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
