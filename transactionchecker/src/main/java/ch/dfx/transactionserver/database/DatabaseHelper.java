package ch.dfx.transactionserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.DepositDTO;

/**
 * 
 */
public class DatabaseHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseHelper.class);

  private PreparedStatement addressByNumberSelectStatement = null;

  private PreparedStatement depositJoinAddressSelectStatement = null;

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

      // Deposit ...
      String depositJoinAddressSelectSql =
          "SELECT"
              + " d.*,"
              + " a1.address AS customer_address,"
              + " a2.address AS deposit_address"
              + " FROM DEPOSIT d"
              + " JOIN ADDRESS a1 ON"
              + " d.customer_address_number = a1.number"
              + " JOIN ADDRESS a2 ON"
              + " d.deposit_address_number = a2.number";
      depositJoinAddressSelectStatement = connection.prepareStatement(depositJoinAddressSelectSql);

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
      depositJoinAddressSelectStatement.close();
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

  /**
   * 
   */
  public List<DepositDTO> getDepositDTOList() throws DfxException {
    LOGGER.trace("getDepositDTOList() ...");

    try {
      List<DepositDTO> depositDTOList = new ArrayList<>();

      ResultSet resultSet = depositJoinAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        DepositDTO depositDTO = new DepositDTO();

        depositDTO.setCustomerAddressNumber(resultSet.getInt("customer_address_number"));
        depositDTO.setCustomerAddress(resultSet.getString("customer_address"));
        depositDTO.setDepositAddressNumber(resultSet.getInt("deposit_address_number"));
        depositDTO.setDepositAddress(resultSet.getString("deposit_address"));
        depositDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        depositDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        depositDTOList.add(depositDTO);
      }

      resultSet.close();

      return depositDTOList;
    } catch (Exception e) {
      throw new DfxException("getDepositDTOList", e);
    }
  }
}
