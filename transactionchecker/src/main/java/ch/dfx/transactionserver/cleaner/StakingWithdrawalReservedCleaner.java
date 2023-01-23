package ch.dfx.transactionserver.cleaner;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.events.MessageEvent;
import ch.dfx.transactionserver.data.StakingWithdrawalReservedDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StakingWithdrawalReservedCleaner {
  private static final Logger LOGGER = LogManager.getLogger(StakingWithdrawalReservedCleaner.class);

  private PreparedStatement stakingWithdrawalReservedDeleteStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StakingWithdrawalReservedCleaner(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;

    this.databaseBlockHelper = databaseBlockHelper;
    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void clean(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("clean()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = databaseBalanceHelper.getStakingWithdrawalReservedDTOList(token);

      for (StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO : stakingWithdrawalReservedDTOList) {
        doCleanup(connection, stakingWithdrawalReservedDTO);
      }

      closeStatements();
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("clean", e);
    } finally {
      LOGGER.debug("[StakingWithdrawalReservedCleaner] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String stakingWithdrawalReservedDeleteSql =
          "DELETE FROM " + TOKEN_NETWORK_SCHEMA + ".staking_withdrawal_reserved"
              + " WHERE token_number=?"
              + " AND withdrawal_id=?"
              + " AND transaction_id=?"
              + " AND customer_address=?";
      stakingWithdrawalReservedDeleteStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingWithdrawalReservedDeleteSql));
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
      stakingWithdrawalReservedDeleteStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void doCleanup(
      @Nonnull Connection connection,
      @Nonnull StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO) {
    try {
      String transactionId = stakingWithdrawalReservedDTO.getTransactionId();

      TransactionDTO transactionDTO = databaseBlockHelper.getTransactionDTOById(transactionId);

      if (null == transactionDTO) {
        checkDuration(stakingWithdrawalReservedDTO);
      } else {
        deleteStakingWithdrawalReservedDTO(connection, stakingWithdrawalReservedDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCleanup", e);
    }
  }

  /**
   * 
   */
  private void checkDuration(@Nonnull StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO) {
    LOGGER.trace("checkDuration()");

    Timestamp createTime = stakingWithdrawalReservedDTO.getCreateTime();

    Duration duration = Duration.between(createTime.toInstant(), Instant.now());
    long hours = duration.toHours();

    if (24 < hours) {
      int tokenNumber = stakingWithdrawalReservedDTO.getTokenNumber();
      Integer withdrawalId = stakingWithdrawalReservedDTO.getWithdrawalId();
      String transactionId = stakingWithdrawalReservedDTO.getTransactionId();
      BigDecimal vout = stakingWithdrawalReservedDTO.getVout();

      String message =
          new StringBuilder()
              .append("Staking Withdrawal Reserved: ").append(hours).append(" hours overtime: ")
              .append("tokenNumber=").append(tokenNumber)
              .append(" / withdrawalId=").append(withdrawalId)
              .append(" / transactionId=").append(transactionId)
              .append(" / vout=").append(vout.toPlainString())
              .toString();
      MessageEventBus.getInstance().postEvent(new MessageEvent(message));

      LOGGER.error(message);
    }
  }

  /**
   * 
   */
  private void deleteStakingWithdrawalReservedDTO(
      @Nonnull Connection connection,
      @Nonnull StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO) throws DfxException {
    LOGGER.trace("deleteStakingWithdrawalReservedDTO()");

    try {
      int tokenNumber = stakingWithdrawalReservedDTO.getTokenNumber();
      Integer withdrawalId = stakingWithdrawalReservedDTO.getWithdrawalId();
      String transactionId = stakingWithdrawalReservedDTO.getTransactionId();
      String customerAddress = stakingWithdrawalReservedDTO.getCustomerAddress();

      LOGGER.debug(
          "[DELETE] Token / Withdrawal Id / Transaction Id / Customer Address: "
              + tokenNumber + " / " + withdrawalId + " / " + transactionId + " / " + customerAddress);

      stakingWithdrawalReservedDeleteStatement.setInt(1, tokenNumber);
      stakingWithdrawalReservedDeleteStatement.setInt(2, withdrawalId);
      stakingWithdrawalReservedDeleteStatement.setString(3, transactionId);
      stakingWithdrawalReservedDeleteStatement.setString(4, customerAddress);

      stakingWithdrawalReservedDeleteStatement.execute();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("deleteStakingWithdrawalReservedDTO: " + e.getMessage(), e);
    }
  }
}
