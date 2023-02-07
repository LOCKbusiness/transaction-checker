package ch.dfx.manager.checker.withdrawal;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.manager.ManagerUtils;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.data.StakingWithdrawalReservedDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StakingBalanceChecker {
  private static final Logger LOGGER = LogManager.getLogger(StakingBalanceChecker.class);

  // ...
  private PreparedStatement stakingWithdrawalReservedSelectByCustomerAddressStatement = null;
  private PreparedStatement stakingWithdrawalReservedInsertStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StakingBalanceChecker(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkStakingBalance(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkStakingBalance()");

    TransactionWithdrawalDTOList checkedTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    TransactionWithdrawalDTOList stakingTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();
    TransactionWithdrawalDTOList yieldmachineTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
      OpenTransactionDTO openTransactionDTO = transactionWithdrawalDTO.getOpenTransactionDTO();
      String assetType = openTransactionDTO.getPayload().getAssetType();

      if ("Coin".equals(assetType)) {
        stakingTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
      } else {
        yieldmachineTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
      }
    }

    checkedTransactionWithdrawalDTOList.addAll(checkStakingBalance(TOKEN_STAKING_SCHEMA, stakingTransactionWithdrawalDTOList));
    checkedTransactionWithdrawalDTOList.addAll(checkStakingBalance(TOKEN_YIELDMACHINE_SCHEMA, yieldmachineTransactionWithdrawalDTOList));

    return checkedTransactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private TransactionWithdrawalDTOList checkStakingBalance(
      @Nonnull String dbSchema,
      @Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkStakingBalance()");

    TransactionWithdrawalDTOList checkedTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    if (!transactionWithdrawalDTOList.isEmpty()) {
      Connection connection = null;

      try {
        connection = databaseManager.openConnection();

        databaseBlockHelper.openStatements(connection);
        databaseBalanceHelper.openStatements(connection, dbSchema);
        openStatements(connection, dbSchema);

        for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
          if (checkStakingBalance(connection, transactionWithdrawalDTO)) {
            checkedTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
          }
        }

        closeStatements();
        databaseBalanceHelper.closeStatements();
        databaseBlockHelper.closeStatements();
      } catch (Exception e) {
        LOGGER.error("checkStakingBalance", e);
      } finally {
        databaseManager.closeConnection(connection);
      }
    }

    return checkedTransactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private void openStatements(
      @Nonnull Connection connection,
      @Nonnull String dbSchema) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String stakingWithdrawalReservedSelectByCustomerAddressSql =
          "SELECT * FROM " + dbSchema + ".staking_withdrawal_reserved WHERE token_number=? AND customer_address=?";
      stakingWithdrawalReservedSelectByCustomerAddressStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingWithdrawalReservedSelectByCustomerAddressSql));

      String stakingWithdrawalReservedInsertSql =
          "INSERT INTO " + dbSchema + ".staking_withdrawal_reserved"
              + " (token_number, withdrawal_id, transaction_id, customer_address, vout)"
              + " VALUES (?, ?, ?, ?, ?)";
      stakingWithdrawalReservedInsertStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingWithdrawalReservedInsertSql));
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
      stakingWithdrawalReservedSelectByCustomerAddressStatement.close();
      stakingWithdrawalReservedInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private boolean checkStakingBalance(
      @Nonnull Connection connection,
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) {
    LOGGER.trace("checkStakingBalance()");

    try {
      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = getStakingWithdrawalReservedDTOList(transactionWithdrawalDTO);

      BigDecimal stakingWithdrawalReservedVout = BigDecimal.ZERO;

      for (StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO : stakingWithdrawalReservedDTOList) {
        stakingWithdrawalReservedVout = stakingWithdrawalReservedVout.add(stakingWithdrawalReservedDTO.getVout());
      }

      // ...
      BigDecimal withdrawalAmount = BigDecimal.ZERO;

      String transactionId = transactionWithdrawalDTO.getOpenTransactionDTO().getId();
      boolean isTransactionIdInReservedList = stakingWithdrawalReservedDTOList.stream().anyMatch(dto -> transactionId.equals(dto.getTransactionId()));

      if (!isTransactionIdInReservedList) {
        PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();
        withdrawalAmount = withdrawalAmount.add(pendingWithdrawalDTO.getAmount());
      }

      BigDecimal totalWithdrawal = stakingWithdrawalReservedVout.add(withdrawalAmount);

      // ...
      BigDecimal stakingBalance = getStakingBalance(transactionWithdrawalDTO);

      // ...
      if (-1 == stakingBalance.compareTo(totalWithdrawal)) {
        ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, "invalid balance");
        return false;
      }

      if (!isTransactionIdInReservedList) {
        insertStakingWithdrawalReserved(connection, transactionWithdrawalDTO);
      }

      transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.BALANCE_CHECKED);

      return true;
    } catch (Exception e) {
      LOGGER.error("checkStakingBalance: " + e.getMessage(), e);

      ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, e.getMessage());
      return false;
    }
  }

  /**
   *  
   */
  private List<StakingWithdrawalReservedDTO> getStakingWithdrawalReservedDTOList(
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) throws DfxException {
    LOGGER.trace("getStakingWithdrawalReservedDTOList()");

    try {
      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = new ArrayList<>();

      TokenEnum token = transactionWithdrawalDTO.getPendingWithdrawalDTO().getToken();

      String customerAddress = transactionWithdrawalDTO.getCustomerAddress();
      stakingWithdrawalReservedSelectByCustomerAddressStatement.setInt(1, token.getNumber());
      stakingWithdrawalReservedSelectByCustomerAddressStatement.setString(2, customerAddress);

      ResultSet resultSet = stakingWithdrawalReservedSelectByCustomerAddressStatement.executeQuery();

      while (resultSet.next()) {
        StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO = new StakingWithdrawalReservedDTO(token.getNumber());

        stakingWithdrawalReservedDTO.setWithdrawalId(resultSet.getInt("withdrawal_id"));
        stakingWithdrawalReservedDTO.setTransactionId(resultSet.getString("transaction_id"));
        stakingWithdrawalReservedDTO.setCustomerAddress(resultSet.getString("customer_address"));
        stakingWithdrawalReservedDTO.setVout(resultSet.getBigDecimal("vout"));

        stakingWithdrawalReservedDTOList.add(stakingWithdrawalReservedDTO);
      }

      resultSet.close();

      return stakingWithdrawalReservedDTOList;
    } catch (Exception e) {
      throw new DfxException("getStakingWithdrawalReservedDTOList", e);
    }
  }

  /**
   * 
   */
  private BigDecimal getStakingBalance(
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) throws DfxException {
    LOGGER.trace("getStakingBalance()");

    BigDecimal stakingBalance = BigDecimal.ZERO;

    String customerAddress = transactionWithdrawalDTO.getCustomerAddress();
    AddressDTO addressDTO = databaseBlockHelper.getAddressDTOByAddress(customerAddress);

    if (null != addressDTO) {
      TokenEnum token = transactionWithdrawalDTO.getPendingWithdrawalDTO().getToken();
      int customerAddressNumber = addressDTO.getNumber();

      List<StakingDTO> stakingDTOList = databaseBalanceHelper.getStakingDTOListByCustomerAddressNumber(token, customerAddressNumber);

      for (StakingDTO stakingDTO : stakingDTOList) {
        stakingBalance = stakingBalance.add(stakingDTO.getVin().subtract(stakingDTO.getVout()));
      }
    }

    return stakingBalance;
  }

  /**
   * 
   */
  private void insertStakingWithdrawalReserved(
      @Nonnull Connection connection,
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) throws DfxException {
    LOGGER.trace("insertStakingWithdrawalReserved()");

    try {
      OpenTransactionDTO openTransactionDTO = transactionWithdrawalDTO.getOpenTransactionDTO();
      PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();

      TokenEnum token = pendingWithdrawalDTO.getToken();

      stakingWithdrawalReservedInsertStatement.setInt(1, token.getNumber());
      stakingWithdrawalReservedInsertStatement.setInt(2, pendingWithdrawalDTO.getId());
      stakingWithdrawalReservedInsertStatement.setString(3, openTransactionDTO.getId());
      stakingWithdrawalReservedInsertStatement.setString(4, transactionWithdrawalDTO.getCustomerAddress());
      stakingWithdrawalReservedInsertStatement.setBigDecimal(5, pendingWithdrawalDTO.getAmount());

      stakingWithdrawalReservedInsertStatement.execute();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("insertStakingWithdrawalReserved: " + e.getMessage(), e);
    }
  }
}
