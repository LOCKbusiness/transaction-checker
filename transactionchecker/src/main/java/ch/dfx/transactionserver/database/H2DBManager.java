package ch.dfx.transactionserver.database;

import java.sql.Connection;

import javax.annotation.Nullable;

import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public interface H2DBManager {

  /**
   *
   */
  Connection openConnection() throws DfxException;

  /**
   *
   */
  void closeConnection(@Nullable Connection connection);

  /**
   * 
   */
  void compact() throws DfxException;
}