package ch.dfx.transactionserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressDTO;

/**
 * 
 */
public class DatabaseHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseHelper.class);

  private PreparedStatement addressByNumberSelectStatement = null;

  /**
   * 
   */
  public DatabaseHelper() {
  }

  /**
   * 
   */
  public void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      // Address ...
      String addressByNumberSelectSql = "SELECT * FROM public.address WHERE number=?";
      addressByNumberSelectStatement = connection.prepareStatement(addressByNumberSelectSql);

    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  public void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements() ...");

    try {
      addressByNumberSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressDTO getAddressDTO(int addressNumber) throws DfxException {
    LOGGER.trace("getAddressDTO() ...");

    try {
      AddressDTO addressDTO = null;

      addressByNumberSelectStatement.setInt(1, addressNumber);

      ResultSet resultSet = addressByNumberSelectStatement.executeQuery();

      if (resultSet.next()) {
        addressDTO = new AddressDTO(
            resultSet.getInt("number"),
            resultSet.getString("address"),
            resultSet.getString("hex"));
      }

      resultSet.close();

      return addressDTO;
    } catch (Exception e) {
      throw new DfxException("getAddressDTO", e);
    }
  }
}
