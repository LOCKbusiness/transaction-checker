package ch.dfx.balance;

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
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositBalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class Balance {
  private static final Logger LOGGER = LogManager.getLogger(Balance.class);

  private PreparedStatement depositSelectStatement = null;

  private PreparedStatement balanceSelectStatement = null;

  private PreparedStatement voutSelectStatement = null;
  private PreparedStatement vinSelectStatement = null;

  private PreparedStatement balanceInsertStatement = null;
  private PreparedStatement balanceUpdateStatement = null;

  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public Balance() {
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public List<DepositBalanceDTO> run() throws DfxException {
    LOGGER.trace("run() ...");

    Connection connection = null;

    try {
      List<DepositBalanceDTO> balanceDTOList = new ArrayList<>();

      connection = H2DBManager.getInstance().openConnection();

      openStatements(connection);
      databaseHelper.openStatements(connection);

      List<DepositDTO> depositDTOList = getDepositDTOList();

      for (DepositDTO depositDTO : depositDTOList) {
        AddressDTO customerAddressDTO = databaseHelper.getAddressDTO(depositDTO.getCustomerAddressNumber());
        AddressDTO depositAddressDTO = databaseHelper.getAddressDTO(depositDTO.getDepositAddressNumber());

        if (null != customerAddressDTO) {
          depositDTO.setCustomerAddress(customerAddressDTO.getAddress());
        }

        if (null != depositAddressDTO) {
          depositDTO.setDepositAddress(depositAddressDTO.getAddress());
        }

        BalanceDTO balanceDTO = calcBalance(connection, depositDTO.getDepositAddressNumber());

        balanceDTOList.add(new DepositBalanceDTO(depositDTO, balanceDTO));
      }

      databaseHelper.closeStatements();
      closeStatements();

      connection.commit();

      return balanceDTOList;
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("run", e);
    } finally {
      H2DBManager.getInstance().closeConnection(connection);
    }
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
      // Deposit ...
      String depositSelectSql = "SELECT * FROM public.deposit";
      depositSelectStatement = connection.prepareStatement(depositSelectSql);

      // Balance ...
      String balanceSelectSql = "SELECT * FROM public.balance WHERE address_number =?";
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
      depositSelectStatement.close();

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
  private List<DepositDTO> getDepositDTOList() throws DfxException {
    LOGGER.trace("getDepositAddressNumberList() ...");

    try {
      List<DepositDTO> depositDTOList = new ArrayList<>();

      ResultSet resultSet = depositSelectStatement.executeQuery();

      while (resultSet.next()) {
        DepositDTO depositDTO = new DepositDTO();

        depositDTO.setCustomerAddressNumber(resultSet.getInt("customer_address_number"));
        depositDTO.setDepositAddressNumber(resultSet.getInt("deposit_address_number"));
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
      balanceInsertStatement.setInt(1, balanceDTO.getAddressNumber());
      balanceInsertStatement.setInt(2, balanceDTO.getBlockNumber());
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
      balanceUpdateStatement.setInt(1, balanceDTO.getBlockNumber());
      balanceUpdateStatement.setInt(2, balanceDTO.getTransactionCount());
      balanceUpdateStatement.setBigDecimal(3, balanceDTO.getVout());
      balanceUpdateStatement.setBigDecimal(4, balanceDTO.getVin());
      balanceUpdateStatement.setInt(5, balanceDTO.getAddressNumber());
      balanceUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("updateBalance", e);
    }
  }
}
