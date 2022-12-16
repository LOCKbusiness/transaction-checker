package ch.dfx.manager.checker.transaction;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class DuplicateChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(DuplicateChecker.class);

  // ...
  private PreparedStatement apiDuplicateCheckSelectStatement = null;
  private PreparedStatement apiDuplicateCheckInsertStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  /**
   * 
   */
  public DuplicateChecker(
      @Nonnull NetworkEnum network,
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider,
      @Nonnull H2DBManager databaseManager) {
    super(apiAccessHandler, dataProvider);

    this.network = network;
    this.databaseManager = databaseManager;
  }

  /**
   * 
   */
  public OpenTransactionDTOList checkDuplicated(@Nonnull OpenTransactionDTOList apiOpenTransactionDTOList) {
    LOGGER.trace("checkDuplicated()");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    // ...
    if (!apiOpenTransactionDTOList.isEmpty()) {
      Connection connection = null;

      try {
        connection = databaseManager.openConnection();

        openStatements(connection);

        for (OpenTransactionDTO apiOpenTransactionDTO : apiOpenTransactionDTOList) {
          if (doCheckInsertApiDuplicate(apiOpenTransactionDTO)) {
            checkedOpenTransactionDTOList.add(apiOpenTransactionDTO);
          }
        }

        closeStatements();

        connection.commit();
      } catch (Exception e) {
        DatabaseUtils.rollback(connection);
        LOGGER.error("checkDuplicated", e);
      } finally {
        databaseManager.closeConnection(connection);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String apiDuplicateCheckSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check WHERE withdrawal_id=? AND transaction_id=?";
      apiDuplicateCheckSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, apiDuplicateCheckSelectSql));

      String apiDuplicateCheckInsertSql = "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".api_duplicate_check (withdrawal_id, transaction_id) VALUES (?, ?)";
      apiDuplicateCheckInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, apiDuplicateCheckInsertSql));
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
      apiDuplicateCheckSelectStatement.close();
      apiDuplicateCheckInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private boolean doCheckInsertApiDuplicate(@Nonnull OpenTransactionDTO apiOpenTransactionDTO) {
    LOGGER.trace("doCheckInsertApiDuplicate()");

    boolean isValid;

    try {
      Integer withdrawalId = getWithdrawalId(apiOpenTransactionDTO);

      if (null == withdrawalId) {
        isValid = true;
      } else {
        isValid = apiDuplicateCheckInsert(withdrawalId, apiOpenTransactionDTO.getId());

        if (!isValid) {
          apiOpenTransactionDTO.setInvalidatedReason("[Withdrawal] ID: " + withdrawalId + " - duplicated");
          sendInvalidated(apiOpenTransactionDTO);
        }
      }
    } catch (Exception e) {
      LOGGER.error("doCheckInsertApiDuplicate", e);
      isValid = false;
    }

    return isValid;
  }

  /**
   * Duplicate Check:
   * withdrawal id x + transaction id y: is allowed to receive multiple times
   * withdrawal id x + transaction id z: is not allowed to receive multiple times
   */
  private boolean apiDuplicateCheckInsert(
      @Nonnull Integer withdrawalId,
      @Nonnull String transactionId) {
    LOGGER.trace("apiDuplicateCheckInsert()");

    boolean isValid;

    try {
      apiDuplicateCheckSelectStatement.setInt(1, withdrawalId);
      apiDuplicateCheckSelectStatement.setString(2, transactionId);

      ResultSet resultSet = apiDuplicateCheckSelectStatement.executeQuery();
      isValid = resultSet.next();
      resultSet.close();

      // ...
      if (!isValid) {
        apiDuplicateCheckInsertStatement.setInt(1, withdrawalId);
        apiDuplicateCheckInsertStatement.setString(2, transactionId);
        apiDuplicateCheckInsertStatement.execute();
      }

      isValid = true;
    } catch (Exception e) {
      LOGGER.info("Duplicate: WithdrawId=" + withdrawalId + " / TransactionId=" + transactionId);
      isValid = false;
    }

    return isValid;
  }
}
