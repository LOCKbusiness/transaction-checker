package ch.dfx.transactionserver.builder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
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

  private PreparedStatement balanceSelectStatement = null;

  private PreparedStatement voutSelectStatement = null;
  private PreparedStatement vinSelectStatement = null;

  private PreparedStatement balanceInsertStatement = null;
  private PreparedStatement balanceUpdateStatement = null;

  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public BalanceBuilder() {
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public List<BalanceDTO> build() throws DfxException {
    LOGGER.trace("build() ...");

    Connection connection = null;

    try {
      connection = H2DBManager.getInstance().openConnection();

      databaseHelper.openStatements(connection);
      openStatements(connection);

      List<BalanceDTO> balanceDTOList = calcLiquidityBalance(connection);
      balanceDTOList.addAll(calcDepositBalance(connection));

      closeStatements();
      databaseHelper.closeStatements();

      connection.commit();

      return balanceDTOList;
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      H2DBManager.getInstance().closeConnection(connection);
    }
  }

  /**
   * 
   */
  private List<BalanceDTO> calcLiquidityBalance(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("calcLiquidityBalance() ...");

    List<BalanceDTO> balanceDTOList = new ArrayList<>();

    List<LiquidityDTO> liquidityDTOList = databaseHelper.getLiquidityDTOList();

    for (LiquidityDTO liquidityDTO : liquidityDTOList) {
      BalanceDTO balanceDTO = calcBalance(connection, liquidityDTO.getAddressNumber());
      balanceDTO.setLiquidityDTO(liquidityDTO);

      balanceDTOList.add(balanceDTO);
    }

    return balanceDTOList;
  }

  /**
   * 
   */
  private List<BalanceDTO> calcDepositBalance(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("calcDepositBalance() ...");

    List<BalanceDTO> balanceDTOList = new ArrayList<>();

    List<DepositDTO> depositDTOList = databaseHelper.getDepositDTOList();

    for (DepositDTO depositDTO : depositDTOList) {
      BalanceDTO balanceDTO = calcBalance(connection, depositDTO.getDepositAddressNumber());
      balanceDTO.setDepositDTO(depositDTO);

      balanceDTOList.add(balanceDTO);
    }

    return balanceDTOList;
  }

  /**
   * 
   */
  private BalanceDTO calcBalance(
      @Nonnull Connection connection,
      @Nonnull Integer addressNumber) throws DfxException {
    LOGGER.trace("calcBalance() ...");

    try {
      BalanceDTO balanceDTO = getBalanceDTO(addressNumber);

      // ...
      int balanceBlockNumber = balanceDTO.getBlockNumber();

      BalanceDTO voutBalanceDTO = calcVout(balanceBlockNumber, addressNumber);
      BalanceDTO vinBalanceDTO = calcVin(balanceBlockNumber, addressNumber);

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

      return balanceDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("calcBalance", e);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      // Balance ...
      String balanceSelectSql = "SELECT * FROM public.balance WHERE address_number=?";
      balanceSelectStatement = connection.prepareStatement(balanceSelectSql);

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
      balanceSelectStatement.close();

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
  private BalanceDTO getBalanceDTO(int addressNumber) throws DfxException {
    LOGGER.trace("getBalanceDTO() ...");

    try {
      BalanceDTO balanceDTO = new BalanceDTO(addressNumber);

      balanceSelectStatement.setInt(1, addressNumber);

      ResultSet resultSet = balanceSelectStatement.executeQuery();

      if (resultSet.next()) {
        balanceDTO.setBlockNumber(DatabaseUtils.getIntOrDefault(resultSet, 2, -1));
        balanceDTO.setTransactionCount(DatabaseUtils.getIntOrDefault(resultSet, 3, 0));
        balanceDTO.setVout(DatabaseUtils.getBigDecimalOrDefault(resultSet, 4, BigDecimal.ZERO));
        balanceDTO.setVin(DatabaseUtils.getBigDecimalOrDefault(resultSet, 5, BigDecimal.ZERO));
      }

      resultSet.close();

      return balanceDTO;
    } catch (Exception e) {
      throw new DfxException("getBalanceDTO", e);
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

      ResultSet resultSet = voutSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceDTO.setBlockNumber(resultSet.getInt(1));
        balanceDTO.addVout(resultSet.getBigDecimal(2));
        balanceDTO.addTransactionCount(1);
      }

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

      ResultSet resultSet = vinSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceDTO.setBlockNumber(resultSet.getInt(1));
        balanceDTO.addVin(resultSet.getBigDecimal(2));
        balanceDTO.addTransactionCount(1);
      }

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
