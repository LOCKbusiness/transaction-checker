package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class BalanceBuilder {
  private static final Logger LOGGER = LogManager.getLogger(BalanceBuilder.class);

  // ...
  private PreparedStatement voutSelectStatement = null;
  private PreparedStatement vinSelectStatement = null;

  private PreparedStatement balanceInsertStatement = null;
  private PreparedStatement balanceUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public BalanceBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
  }

  /**
   * 
   */
  public void build(@Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBalanceHelper.openStatements(connection);
      openStatements(connection);

      calcStakingAddressBalance(connection, token);
      calcDepositBalance(connection, token);

      closeStatements();
      databaseBalanceHelper.closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[BalanceBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Vout ...
      String voutSelectSql =
          "SELECT block_number, vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out"
              + " WHERE block_number>? AND address_number=?";
      voutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, voutSelectSql));

      // Vin ...
      String vinSelectSql =
          "SELECT block_number, vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in"
              + " WHERE block_number>? AND address_number=?";
      vinSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, vinSelectSql));

      // Balance ...
      String balanceInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".balance"
              + " (token_number, address_number, block_number, transaction_count, vout, vin)"
              + " VALUES(?, ?, ?, ?, ?, ?)";
      balanceInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, balanceInsertSql));

      String balanceUpdateSql =
          "UPDATE " + TOKEN_NETWORK_SCHEMA + ".balance"
              + " SET block_number=?, transaction_count=?, vout=?, vin=?"
              + " WHERE token_number=? AND address_number=?";
      balanceUpdateStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, balanceUpdateSql));
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements()");

    try {
      voutSelectStatement.close();
      vinSelectStatement.close();

      balanceInsertStatement.close();
      balanceUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void calcStakingAddressBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("calcStakingAddressBalance()");

    Set<Integer> stakingAddressNumberSet = new HashSet<>();

    List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList(token);

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      stakingAddressNumberSet.add(stakingAddressDTO.getLiquidityAddressNumber());
      stakingAddressNumberSet.add(stakingAddressDTO.getRewardAddressNumber());
    }

    for (int stakingAddressNumber : stakingAddressNumberSet) {
      if (-1 != stakingAddressNumber) {
        calcBalance(connection, token, stakingAddressNumber);
      }
    }
  }

  /**
   * 
   */
  private void calcDepositBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("calcDepositBalance()");

    List<DepositDTO> depositDTOList = databaseBalanceHelper.getDepositDTOList(token);

    for (DepositDTO depositDTO : depositDTOList) {
      calcBalance(connection, token, depositDTO.getDepositAddressNumber());
    }
  }

  /**
   * 
   */
  private void calcBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcBalance()");

    try {
      BalanceDTO balanceDTO = databaseBalanceHelper.getBalanceDTOByAddressNumber(token, addressNumber);

      if (null == balanceDTO) {
        balanceDTO = new BalanceDTO(token.getNumber(), addressNumber);
      }

      // ...
      int balanceBlockNumber = balanceDTO.getBlockNumber();

      // ...
      BalanceDTO voutBalanceDTO = calcVout(balanceBlockNumber, token, addressNumber);
      BalanceDTO vinBalanceDTO = calcVin(balanceBlockNumber, token, addressNumber);

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
      @Nonnull TokenEnum token,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcVout()");

    try {
      voutSelectStatement.setInt(1, balanceBlockNumber);
      voutSelectStatement.setInt(2, addressNumber);

      BalanceDTO balanceDTO = new BalanceDTO(token.getNumber(), addressNumber);

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
      @Nonnull TokenEnum token,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcVin()");

    try {
      vinSelectStatement.setInt(1, balanceBlockNumber);
      vinSelectStatement.setInt(2, addressNumber);

      BalanceDTO balanceDTO = new BalanceDTO(token.getNumber(), addressNumber);

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
    LOGGER.trace("insertBalance()");

    try {
      int tokenNumber = balanceDTO.getTokenNumber();
      int addressNumber = balanceDTO.getAddressNumber();
      int blockNumber = balanceDTO.getBlockNumber();

      LOGGER.debug(
          "[INSERT] Token / Address / Block: "
              + tokenNumber + " / " + addressNumber + " / " + blockNumber);

      balanceInsertStatement.setInt(1, tokenNumber);
      balanceInsertStatement.setInt(2, addressNumber);
      balanceInsertStatement.setInt(3, blockNumber);
      balanceInsertStatement.setInt(4, balanceDTO.getTransactionCount());
      balanceInsertStatement.setBigDecimal(5, balanceDTO.getVout());
      balanceInsertStatement.setBigDecimal(6, balanceDTO.getVin());
      balanceInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertBalance", e);
    }
  }

  /**
   * 
   */
  private void updateBalance(@Nonnull BalanceDTO balanceDTO) throws DfxException {
    LOGGER.trace("updateBalance()");

    try {
      int tokenNumber = balanceDTO.getTokenNumber();
      int addressNumber = balanceDTO.getAddressNumber();
      int blockNumber = balanceDTO.getBlockNumber();

      LOGGER.debug(
          "[UPDATE] Token / Address / Block: "
              + tokenNumber + " / " + addressNumber + " / " + blockNumber);

      balanceUpdateStatement.setInt(1, blockNumber);
      balanceUpdateStatement.setInt(2, balanceDTO.getTransactionCount());
      balanceUpdateStatement.setBigDecimal(3, balanceDTO.getVout());
      balanceUpdateStatement.setBigDecimal(4, balanceDTO.getVin());

      balanceUpdateStatement.setInt(5, tokenNumber);
      balanceUpdateStatement.setInt(6, addressNumber);
      balanceUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("updateBalance", e);
    }
  }
}
