package ch.dfx.transactionserver.ymbuilder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

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
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class YmBalanceBuilder {
  private static final Logger LOGGER = LogManager.getLogger(YmBalanceBuilder.class);

  // ...
  private PreparedStatement outAmountSelectStatement = null;
  private PreparedStatement inAmountSelectStatement = null;

  private PreparedStatement balanceInsertStatement = null;
  private PreparedStatement balanceUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public YmBalanceBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;

    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void build(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      calcStakingAddressBalance(connection, token);
      calcDepositBalance(connection, token);

      closeStatements();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      LOGGER.debug("[YmBalanceBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Out ...
      String outAmountSelectSql =
          "SELECT"
              + " MAX(block_number) AS max_block_number,"
              + " SUM(amount) AS sum,"
              + " COUNT(amount) AS count"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out"
              + " WHERE block_number>? AND address_number=? AND token_number=?";
      outAmountSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, outAmountSelectSql));

      // In ...
      String inAmountSelectSql =
          "SELECT"
              + " MAX(block_number) AS max_block_number,"
              + " SUM(amount) AS sum,"
              + " COUNT(amount) AS count"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in"
              + " WHERE block_number>? AND address_number=? AND token_number=?";
      inAmountSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, inAmountSelectSql));

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
      outAmountSelectStatement.close();
      inAmountSelectStatement.close();

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
      BalanceDTO voutBalanceDTO = calcOutAmount(balanceBlockNumber, token, addressNumber);
      BalanceDTO vinBalanceDTO = calcInAmount(balanceBlockNumber, token, addressNumber);

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
      } else if (balanceDTO.isInternalStateChanged()) {
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
  private BalanceDTO calcOutAmount(
      int balanceBlockNumber,
      @Nonnull TokenEnum token,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcOutAmount()");

    try {
      outAmountSelectStatement.setInt(1, balanceBlockNumber);
      outAmountSelectStatement.setInt(2, addressNumber);
      outAmountSelectStatement.setInt(3, token.getNumber());

      BalanceDTO balanceDTO = new BalanceDTO(token.getNumber(), addressNumber);

      ResultSet resultSet = outAmountSelectStatement.executeQuery();

      if (resultSet.next()) {
        int blockNumber = resultSet.getInt("max_block_number");

        if (!resultSet.wasNull()) {
          balanceDTO.setBlockNumber(blockNumber);
          balanceDTO.addVout(resultSet.getBigDecimal("sum"));
          balanceDTO.setTransactionCount(resultSet.getInt("count"));
        }
      }

      resultSet.close();

      return balanceDTO;
    } catch (Exception e) {
      throw new DfxException("calcOutAmount", e);
    }
  }

  /**
   * 
   */
  private BalanceDTO calcInAmount(
      int balanceBlockNumber,
      @Nonnull TokenEnum token,
      int addressNumber) throws DfxException {
    LOGGER.trace("calcInAmount()");

    try {
      inAmountSelectStatement.setInt(1, balanceBlockNumber);
      inAmountSelectStatement.setInt(2, addressNumber);
      inAmountSelectStatement.setInt(3, token.getNumber());

      BalanceDTO balanceDTO = new BalanceDTO(token.getNumber(), addressNumber);

      ResultSet resultSet = inAmountSelectStatement.executeQuery();

      if (resultSet.next()) {
        int blockNumber = resultSet.getInt("max_block_number");

        if (!resultSet.wasNull()) {
          balanceDTO.setBlockNumber(blockNumber);
          balanceDTO.addVin(resultSet.getBigDecimal("sum"));
          balanceDTO.setTransactionCount(resultSet.getInt("count"));
        }
      }

      resultSet.close();

      return balanceDTO;
    } catch (Exception e) {
      throw new DfxException("calcInAmount", e);
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
