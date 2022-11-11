package ch.dfx.transactionserver.cleaner;

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

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.events.MessageEvent;
import ch.dfx.transactionserver.data.StakingWithdrawalReservedDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StakingWithdrawalReservedCleaner {
  private static final Logger LOGGER = LogManager.getLogger(StakingWithdrawalReservedCleaner.class);

  private PreparedStatement stakingWithdrawalReservedDeleteStatement = null;

  private final H2DBManager databaseManager;
  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public StakingWithdrawalReservedCleaner(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public void clean() {
    LOGGER.trace("clean()");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseHelper.openStatements(connection);
      openStatements(connection);

      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = databaseHelper.getStakingWithdrawalReservedDTOList();

      for (StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO : stakingWithdrawalReservedDTOList) {
        doCleanup(connection, stakingWithdrawalReservedDTO);
      }

      closeStatements();
      databaseHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("clean", e);
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
      String stakingWithdrawalReservedDeleteSql =
          "DELETE FROM public.staking_withdrawal_reserved"
              + " WHERE withdrawal_id=?"
              + " AND transaction_id=?"
              + " AND customer_address=?";
      stakingWithdrawalReservedDeleteStatement = connection.prepareStatement(stakingWithdrawalReservedDeleteSql);
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

      TransactionDTO transactionDTO = databaseHelper.getTransactionDTOById(transactionId);

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
    LOGGER.trace("checkDuration() ...");

    Timestamp createTime = stakingWithdrawalReservedDTO.getCreateTime();

    Duration duration = Duration.between(createTime.toInstant(), Instant.now());
    long hours = duration.toHours();

    if (24 < hours) {
      Integer withdrawalId = stakingWithdrawalReservedDTO.getWithdrawalId();
      String transactionId = stakingWithdrawalReservedDTO.getTransactionId();
      BigDecimal vout = stakingWithdrawalReservedDTO.getVout();

      String message =
          new StringBuilder()
              .append("Staking Withdrawal Reserved: ").append(hours).append(" hours overtime: ")
              .append("withdrawalId=").append(withdrawalId)
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
    LOGGER.trace("deleteStakingWithdrawalReservedDTO() ...");

    try {
      stakingWithdrawalReservedDeleteStatement.setInt(1, stakingWithdrawalReservedDTO.getWithdrawalId());
      stakingWithdrawalReservedDeleteStatement.setString(2, stakingWithdrawalReservedDTO.getTransactionId());
      stakingWithdrawalReservedDeleteStatement.setString(3, stakingWithdrawalReservedDTO.getCustomerAddress());

      stakingWithdrawalReservedDeleteStatement.execute();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("deleteStakingWithdrawalReservedDTO: " + e.getMessage(), e);
    }
  }
}
