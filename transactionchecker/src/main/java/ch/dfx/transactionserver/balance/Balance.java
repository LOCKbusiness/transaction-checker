package ch.dfx.transactionserver.balance;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.balance.data.BalanceData;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * Check Address:
 * df1qh4fhv6kf25ggwl3y3qstw5gtu6k3xtflx3cxt9
 * 
 * dSPPfAPY8BA3TQdqfZRnzJ7212HPWunDms
 * df1qxylxrcxg0gn5zwvyjydxhzr3lttlarklwrl4n9
 * df1qd07y226vhfluma0axy2j8fnsvcgw8p0p4fd0fu
 * 8PzFNiwZQop5Cgqi844GTppWZq8QUtUdZF
 * dJs8vikW87E1M3e5oe4N6hUpBs89Dhh77S
 */
public class Balance {
  private static final Logger LOGGER = LogManager.getLogger(Balance.class);

  private PreparedStatement addressSelectStatement = null;
  private PreparedStatement balanceSelectStatement = null;

  private PreparedStatement voutSelectStatement = null;
  private PreparedStatement vinSelectStatement = null;

  private PreparedStatement balanceInsertStatement = null;
  private PreparedStatement balanceUpdateStatement = null;

  /**
   * 
   */
  public Balance() {
  }

  /**
   * 
   */
  public BalanceData calcBalance(@Nonnull String address) throws DfxException {
    LOGGER.trace("calcBalance() ...");

    Connection connection = null;

    try {
      connection = H2DBManager.getInstance().openConnection();
      openStatements(connection);

      int addressNumber = getAddressNumber(address);
      BalanceData balanceData = getBalanceData(addressNumber);

      // ...
      int balanceBlockNumber = balanceData.getBlockNumber();

      BalanceData voutBalanceData = calcVout(balanceBlockNumber, addressNumber);
      BalanceData vinBalanceData = calcVin(balanceBlockNumber, addressNumber);

      int maxBalanceBlockNumber = balanceBlockNumber;
      maxBalanceBlockNumber = Math.max(maxBalanceBlockNumber, voutBalanceData.getBlockNumber());
      maxBalanceBlockNumber = Math.max(maxBalanceBlockNumber, vinBalanceData.getBlockNumber());

      // ...
      balanceData.setBlockNumber(maxBalanceBlockNumber);
      balanceData.addTransactionCount(voutBalanceData.getTransactionCount());
      balanceData.addTransactionCount(vinBalanceData.getTransactionCount());
      balanceData.addVout(voutBalanceData.getVout());
      balanceData.addVin(vinBalanceData.getVin());

      // ...
      if (-1 == balanceBlockNumber) {
        insertBalance(balanceData);
      } else if (maxBalanceBlockNumber > balanceBlockNumber) {
        updateBalance(balanceData);
      }

      closeStatements();

      connection.commit();

      return balanceData;
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("calcBalance", e);
    } finally {
      H2DBManager.getInstance().closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      // Address ...
      String addressSelectSql = "SELECT number FROM public.address WHERE address=?";
      addressSelectStatement = connection.prepareStatement(addressSelectSql);

      // Balance ...
      String balanceSelectSql = "SELECT * FROM public.balances WHERE address_number =?";
      balanceSelectStatement = connection.prepareStatement(balanceSelectSql);

      String balanceInsertSql = "INSERT INTO public.balances (address_number, block_number, transaction_count, vout, vin) VALUES(?, ?, ?, ?, ?)";
      balanceInsertStatement = connection.prepareStatement(balanceInsertSql);

      String balanceUpdateSql = "UPDATE public.balances SET block_number=?, transaction_count=?, vout=?, vin=? WHERE address_number=?";
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
      addressSelectStatement.close();
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
  private int getAddressNumber(@Nonnull String address) throws DfxException {
    LOGGER.trace("getAddressNumber() ...");

    try {
      int addressNumber = -1;

      addressSelectStatement.setString(1, address);

      ResultSet resultSet = addressSelectStatement.executeQuery();

      if (resultSet.next()) {
        addressNumber = resultSet.getInt(1);

        if (resultSet.wasNull()) {
          addressNumber = -1;
        }

        LOGGER.debug("Address Number: " + addressNumber);
      }

      resultSet.close();

      if (-1 == addressNumber) {
        throw new DfxException("Unknown address: " + address);
      }

      return addressNumber;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressNumber", e);
    }
  }

  /**
   * 
   */
  private BalanceData getBalanceData(int addressNumber) throws DfxException {
    LOGGER.trace("getBalanceData() ...");

    try {
      BalanceData balanceData = new BalanceData(addressNumber);

      balanceSelectStatement.setInt(1, addressNumber);

      ResultSet resultSet = balanceSelectStatement.executeQuery();

      if (resultSet.next()) {
        balanceData.setBlockNumber(DatabaseUtils.getIntOrDefault(resultSet, 2, -1));
        balanceData.setTransactionCount(DatabaseUtils.getIntOrDefault(resultSet, 3, 0));
        balanceData.setVout(DatabaseUtils.getBigDecimalOrDefault(resultSet, 4, BigDecimal.ZERO));
        balanceData.setVin(DatabaseUtils.getBigDecimalOrDefault(resultSet, 5, BigDecimal.ZERO));
      }

      resultSet.close();

      return balanceData;
    } catch (Exception e) {
      throw new DfxException("getBalanceData", e);
    }
  }

  /**
   * 
   */
  private BalanceData calcVout(
      int balanceBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcVout() ...");

    try {
      voutSelectStatement.setInt(1, balanceBlockNumber);
      voutSelectStatement.setInt(2, addressNumber);

      BalanceData balanceData = new BalanceData(addressNumber);

      ResultSet resultSet = voutSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceData.setBlockNumber(resultSet.getInt(1));
        balanceData.addVout(resultSet.getBigDecimal(2));
        balanceData.addTransactionCount(1);
      }

      resultSet.close();

      return balanceData;
    } catch (Exception e) {
      throw new DfxException("calcVout", e);
    }
  }

  /**
   * 
   */
  private BalanceData calcVin(
      int balanceBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcVin() ...");

    try {
      vinSelectStatement.setInt(1, balanceBlockNumber);
      vinSelectStatement.setInt(2, addressNumber);

      BalanceData balanceData = new BalanceData(addressNumber);

      ResultSet resultSet = vinSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceData.setBlockNumber(resultSet.getInt(1));
        balanceData.addVin(resultSet.getBigDecimal(2));
        balanceData.addTransactionCount(1);
      }

      resultSet.close();

      return balanceData;
    } catch (Exception e) {
      throw new DfxException("calcVin", e);
    }
  }

  /**
   * 
   */
  private void insertBalance(@Nonnull BalanceData balanceData) throws DfxException {
    LOGGER.trace("insertBalance() ...");

    try {
      balanceInsertStatement.setInt(1, balanceData.getAddressNumber());
      balanceInsertStatement.setInt(2, balanceData.getBlockNumber());
      balanceInsertStatement.setInt(3, balanceData.getTransactionCount());
      balanceInsertStatement.setBigDecimal(4, balanceData.getVout());
      balanceInsertStatement.setBigDecimal(5, balanceData.getVin());
      balanceInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertBalance", e);
    }
  }

  /**
   * 
   */
  private void updateBalance(@Nonnull BalanceData balanceData) throws DfxException {
    LOGGER.trace("updateBalance() ...");

    try {
      balanceUpdateStatement.setInt(1, balanceData.getBlockNumber());
      balanceUpdateStatement.setInt(2, balanceData.getTransactionCount());
      balanceUpdateStatement.setBigDecimal(3, balanceData.getVout());
      balanceUpdateStatement.setBigDecimal(4, balanceData.getVin());
      balanceUpdateStatement.setInt(5, balanceData.getAddressNumber());
      balanceUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("updateBalance", e);
    }
  }
}
