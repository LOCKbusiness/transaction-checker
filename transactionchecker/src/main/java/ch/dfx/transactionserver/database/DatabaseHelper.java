package ch.dfx.transactionserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;

/**
 * 
 */
public class DatabaseHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseHelper.class);

  private PreparedStatement addressByNumberSelectStatement = null;

  private PreparedStatement liquidityJoinAddressSelectStatement = null;
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

      // Liquidity ...
      String liquidityJoinAddressSelectSql =
          "SELECT"
              + " l.*,"
              + " a.address"
              + " FROM"
              + " LIQUIDITY l"
              + " JOIN ADDRESS a ON"
              + " l.address_number = a.number"
              + "";
      liquidityJoinAddressSelectStatement = connection.prepareStatement(liquidityJoinAddressSelectSql);

      // Deposit ...
      String depositJoinAddressSelectSql =
          "SELECT"
              + " d.*,"
              + " a1.address AS liquidity_address,"
              + " a2.address AS deposit_address,"
              + " a3.address AS customer_address"
              + " FROM DEPOSIT d"
              + " JOIN ADDRESS a1 ON"
              + " d.liquidity_address_number = a1.number"
              + " JOIN ADDRESS a2 ON"
              + " d.deposit_address_number = a2.number"
              + " JOIN ADDRESS a3 ON"
              + " d.customer_address_number = a3.number";
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

      liquidityJoinAddressSelectStatement.close();
      depositJoinAddressSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public AddressDTO getAddressDTOByNumber(int addressNumber) throws DfxException {
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

      if (null == addressDTO) {
        throw new DfxException("Address not found by number " + addressNumber);
      }

      return addressDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressDTO", e);
    }
  }

  /**
   * 
   */
  public List<LiquidityDTO> getLiquidityDTOList() throws DfxException {
    LOGGER.trace("getLiquidityDTOList() ...");

    try {
      List<LiquidityDTO> liquidityDTOList = new ArrayList<>();

      ResultSet resultSet = liquidityJoinAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        LiquidityDTO liquidityDTO = new LiquidityDTO();

        liquidityDTO.setAddressNumber(resultSet.getInt("address_number"));
        liquidityDTO.setAddress(resultSet.getString("address"));
        liquidityDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        liquidityDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        liquidityDTOList.add(liquidityDTO);
      }
      return liquidityDTOList;
    } catch (Exception e) {
      throw new DfxException("getLiquidityDTOList", e);
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

        depositDTO.setLiquidityAddressNumber(resultSet.getInt("liquidity_address_number"));
        depositDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));
        depositDTO.setDepositAddressNumber(resultSet.getInt("deposit_address_number"));
        depositDTO.setDepositAddress(resultSet.getString("deposit_address"));
        depositDTO.setCustomerAddressNumber(resultSet.getInt("customer_address_number"));
        depositDTO.setCustomerAddress(resultSet.getString("customer_address"));
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
