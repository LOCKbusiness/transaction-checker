package ch.dfx.transactionserver.builder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class BalanceBuilder {
  private static final Logger LOGGER = LogManager.getLogger(BalanceBuilder.class);

  private PreparedStatement voutSelectStatement = null;
  private PreparedStatement vinSelectStatement = null;

  private PreparedStatement balanceInsertStatement = null;
  private PreparedStatement balanceUpdateStatement = null;

  // ...
  private final H2DBManager databaseManager;
  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public BalanceBuilder(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public void build() throws DfxException {
    LOGGER.debug("build() ...");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseHelper.openStatements(connection);
      openStatements(connection);

      calcLiquidityBalance(connection);
      calcDepositBalance(connection);

      closeStatements();
      databaseHelper.closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      // Balance ...
      String balanceInsertSql = "INSERT INTO public.balance (address_number, block_number, transaction_count, vout, vin) VALUES(?, ?, ?, ?, ?)";
      balanceInsertStatement = connection.prepareStatement(balanceInsertSql);

      String balanceUpdateSql = "UPDATE public.balance SET block_number=?, transaction_count=?, vout=?, vin=? WHERE address_number=?";
      balanceUpdateStatement = connection.prepareStatement(balanceUpdateSql);

      // Vout ...
      String voutSelectSql = "SELECT block_number, vout FROM public.address_transaction_out WHERE block_number>? and address_number=?";
      voutSelectStatement = connection.prepareStatement(voutSelectSql);

      // Vin ...
      String vinSelectSql = "SELECT block_number, vin FROM public.address_transaction_in WHERE block_number>? and address_number=?";
      vinSelectStatement = connection.prepareStatement(vinSelectSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements() ...");

    try {
      balanceInsertStatement.close();
      balanceUpdateStatement.close();

      voutSelectStatement.close();
      vinSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void calcLiquidityBalance(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("calcLiquidityBalance() ...");

    List<LiquidityDTO> liquidityDTOList = databaseHelper.getLiquidityDTOList();

    for (LiquidityDTO liquidityDTO : liquidityDTOList) {
      calcBalance(connection, liquidityDTO.getAddressNumber());
    }
  }

  /**
   * 
   */
  private void calcDepositBalance(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("calcDepositBalance() ...");

    List<DepositDTO> depositDTOList = databaseHelper.getDepositDTOList();

    for (DepositDTO depositDTO : depositDTOList) {
      calcBalance(connection, depositDTO.getDepositAddressNumber());
    }
  }

  /**
   * 
   */
  private void calcBalance(
      @Nonnull Connection connection,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcBalance() ...");

    try {
      BalanceDTO balanceDTO = databaseHelper.getBalanceDTOByAddressNumber(addressNumber);

      if (null == balanceDTO) {
        balanceDTO = new BalanceDTO(addressNumber);
      }

      // ...
      int balanceBlockNumber = balanceDTO.getBlockNumber();

      // ...
      BalanceDTO voutBalanceDTO = calcVout(balanceBlockNumber, addressNumber);
      BalanceDTO vinBalanceDTO = calcVin(balanceBlockNumber, addressNumber);

      // ...
      int maxBalanceBlockNumber = balanceBlockNumber;
      maxBalanceBlockNumber = Math.max(maxBalanceBlockNumber, voutBalanceDTO.getBlockNumber());
      maxBalanceBlockNumber = Math.max(maxBalanceBlockNumber, vinBalanceDTO.getBlockNumber());

      // ...
      balanceDTO.setBlockNumber(maxBalanceBlockNumber);
      balanceDTO.addTransactionCount(voutBalanceDTO.getTransactionCount());
      balanceDTO.addTransactionCount(vinBalanceDTO.getTransactionCount());
      balanceDTO.addVout(voutBalanceDTO.getVout());
      balanceDTO.addVin(vinBalanceDTO.getVin());

      // ...
      if (-1 == balanceBlockNumber) {
        insertBalance(balanceDTO);
      } else if (maxBalanceBlockNumber > balanceBlockNumber) {
        updateBalance(balanceDTO);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("calcBalance", e);
    }
  }

  /**
   * 
   */
  private BalanceDTO calcVout(
      int balanceBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcVout() ...");

    try {
      voutSelectStatement.setInt(1, balanceBlockNumber);
      voutSelectStatement.setInt(2, addressNumber);

      BalanceDTO balanceDTO = new BalanceDTO(addressNumber);

      int maxBlockNumber = -1;

      ResultSet resultSet = voutSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceDTO.addVout(resultSet.getBigDecimal("vout"));
        balanceDTO.addTransactionCount(1);

        maxBlockNumber = Math.max(maxBlockNumber, resultSet.getInt("block_number"));
      }

      balanceDTO.setBlockNumber(maxBlockNumber);

      resultSet.close();

      return balanceDTO;
    } catch (Exception e) {
      throw new DfxException("calcVout", e);
    }
  }

  /**
   * 
   */
  private BalanceDTO calcVin(
      int balanceBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcVin() ...");

    try {
      vinSelectStatement.setInt(1, balanceBlockNumber);
      vinSelectStatement.setInt(2, addressNumber);

      BalanceDTO balanceDTO = new BalanceDTO(addressNumber);

      int maxBlockNumber = -1;

      ResultSet resultSet = vinSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceDTO.addVin(resultSet.getBigDecimal("vin"));
        balanceDTO.addTransactionCount(1);

        maxBlockNumber = Math.max(maxBlockNumber, resultSet.getInt("block_number"));
      }

      balanceDTO.setBlockNumber(maxBlockNumber);

      resultSet.close();

      return balanceDTO;
    } catch (Exception e) {
      throw new DfxException("calcVin", e);
    }
  }

  /**
   * 
   */
  private void insertBalance(@Nonnull BalanceDTO balanceDTO) throws DfxException {
    LOGGER.trace("insertBalance() ...");

    try {
      int addressNumber = balanceDTO.getAddressNumber();
      int blockNumber = balanceDTO.getBlockNumber();

      LOGGER.debug(
          "[INSERT] Address / Block: "
              + addressNumber + " / " + blockNumber);

      balanceInsertStatement.setInt(1, addressNumber);
      balanceInsertStatement.setInt(2, blockNumber);
      balanceInsertStatement.setInt(3, balanceDTO.getTransactionCount());
      balanceInsertStatement.setBigDecimal(4, balanceDTO.getVout());
      balanceInsertStatement.setBigDecimal(5, balanceDTO.getVin());
      balanceInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertBalance", e);
    }
  }

  /**
   * 
   */
  private void updateBalance(@Nonnull BalanceDTO balanceDTO) throws DfxException {
    LOGGER.trace("updateBalance() ...");

    try {
      int addressNumber = balanceDTO.getAddressNumber();
      int blockNumber = balanceDTO.getBlockNumber();

      LOGGER.debug(
          "[UPDATE] Address / Block: "
              + addressNumber + " / " + blockNumber);

      balanceUpdateStatement.setInt(1, blockNumber);
      balanceUpdateStatement.setInt(2, balanceDTO.getTransactionCount());
      balanceUpdateStatement.setBigDecimal(3, balanceDTO.getVout());
      balanceUpdateStatement.setBigDecimal(4, balanceDTO.getVin());
      balanceUpdateStatement.setInt(5, addressNumber);
      balanceUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("updateBalance", e);
    }
  }
}
