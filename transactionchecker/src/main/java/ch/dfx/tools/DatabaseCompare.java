package ch.dfx.tools;

import java.sql.Connection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.data.DatabaseConnectionData;
import ch.dfx.tools.data.DatabaseData;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DatabaseDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;

/**
 * Compare table content from remote and local:
 * 
 * - MASTERNODE_WHITELIST
 * - STAKING_ADDRESS
 * - BALANCE
 * - DEPOSIT
 * - STAKING
 */
public class DatabaseCompare extends DatabaseTool {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseCompare.class);

  /**
   * 
   */
  public DatabaseCompare() {
  }

  /**
   * 
   */
  public void compare() throws DfxException {
    LOGGER.trace("compare()");

    Connection localConnection = null;
    Connection remoteConnection = null;

    try {
      DatabaseConnectionData databaseConnectionData = getDatabaseConnectionData();

      DatabaseData localDatabaseData = databaseConnectionData.getLocalDatabaseData();
      localConnection = openConnection(localDatabaseData);

      DatabaseData remoteDatabaseData = databaseConnectionData.getRemoteDatabaseData();
      remoteConnection = openConnection(remoteDatabaseData);

      doCompare(localConnection, remoteConnection);
    } finally {
      closeConnection(localConnection);
      closeConnection(remoteConnection);
    }
  }

  /**
   * 
   */
  private void doCompare(
      @Nonnull Connection localConnection,
      @Nonnull Connection remoteConnection) throws DfxException {
    LOGGER.trace("doCompare()");

    DatabaseHelper localDatabaseHelper = new DatabaseHelper();
    DatabaseHelper remoteDatabaseHelper = new DatabaseHelper();

    localDatabaseHelper.openStatements(localConnection);
    remoteDatabaseHelper.openStatements(remoteConnection);

    compareMasternodeWhitelist(localDatabaseHelper, remoteDatabaseHelper);
    compareStakingAddress(localDatabaseHelper, remoteDatabaseHelper);

    compareBalance(localDatabaseHelper, remoteDatabaseHelper);
    compareDeposit(localDatabaseHelper, remoteDatabaseHelper);
    compareStaking(localDatabaseHelper, remoteDatabaseHelper);

    localDatabaseHelper.closeStatements();
    remoteDatabaseHelper.closeStatements();
  }

  /**
   * 
   */
  private void compareMasternodeWhitelist(
      @Nonnull DatabaseHelper localDatabaseHelper,
      @Nonnull DatabaseHelper remoteDatabaseHelper) throws DfxException {
    LOGGER.trace("compareMasternodeWhitelist()");

    List<MasternodeWhitelistDTO> localMasternodeWhitelistDTOList = localDatabaseHelper.getMasternodeWhitelistDTOList();
    List<MasternodeWhitelistDTO> remoteMasternodeWhitelistDTOList = remoteDatabaseHelper.getMasternodeWhitelistDTOList();

    int localSize = localMasternodeWhitelistDTOList.size();
    int remoteSize = remoteMasternodeWhitelistDTOList.size();

    if (localMasternodeWhitelistDTOList.toString().equals(remoteMasternodeWhitelistDTOList.toString())) {
      LOGGER.info("[MasternodeWhitelistDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[MasternodeWhitelistDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localMasternodeWhitelistDTOList, remoteMasternodeWhitelistDTOList);
    }
  }

  /**
   * 
   */
  private void compareStakingAddress(
      @Nonnull DatabaseHelper localDatabaseHelper,
      @Nonnull DatabaseHelper remoteDatabaseHelper) throws DfxException {
    LOGGER.trace("compareStakingAddress()");

    List<StakingAddressDTO> localStakingAddressDTOList = localDatabaseHelper.getStakingAddressDTOList();
    List<StakingAddressDTO> remoteStakingAddressDTOList = remoteDatabaseHelper.getStakingAddressDTOList();

    int localSize = localStakingAddressDTOList.size();
    int remoteSize = remoteStakingAddressDTOList.size();

    if (localStakingAddressDTOList.toString().equals(remoteStakingAddressDTOList.toString())) {
      LOGGER.info("[StakingAddressDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[StakingAddressDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localStakingAddressDTOList, remoteStakingAddressDTOList);
    }
  }

  /**
   * 
   */
  private void compareBalance(
      @Nonnull DatabaseHelper localDatabaseHelper,
      @Nonnull DatabaseHelper remoteDatabaseHelper) throws DfxException {
    LOGGER.trace("compareBalance()");

    List<BalanceDTO> localBalanceDTOList = localDatabaseHelper.getBalanceDTOList();
    List<BalanceDTO> remoteBalanceDTOList = remoteDatabaseHelper.getBalanceDTOList();

    int localSize = localBalanceDTOList.size();
    int remoteSize = remoteBalanceDTOList.size();

    if (localBalanceDTOList.toString().equals(remoteBalanceDTOList.toString())) {
      LOGGER.info("[BalanceDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[BalanceDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localBalanceDTOList, remoteBalanceDTOList);
    }
  }

  /**
   * 
   */
  private void compareDeposit(
      @Nonnull DatabaseHelper localDatabaseHelper,
      @Nonnull DatabaseHelper remoteDatabaseHelper) throws DfxException {
    LOGGER.trace("compareDeposit()");

    List<DepositDTO> localDepositDTOList = localDatabaseHelper.getDepositDTOList();
    List<DepositDTO> remoteDepositDTOList = remoteDatabaseHelper.getDepositDTOList();

    int localSize = localDepositDTOList.size();
    int remoteSize = remoteDepositDTOList.size();

    if (localDepositDTOList.toString().equals(remoteDepositDTOList.toString())) {
      LOGGER.info("[DepositDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[DepositDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localDepositDTOList, remoteDepositDTOList);
    }
  }

  /**
   * 
   */
  private void compareStaking(
      @Nonnull DatabaseHelper localDatabaseHelper,
      @Nonnull DatabaseHelper remoteDatabaseHelper) throws DfxException {
    LOGGER.trace("compareStaking()");

    List<StakingDTO> localStakingDTOList = localDatabaseHelper.getStakingDTOList();
    List<StakingDTO> remoteStakingDTOList = remoteDatabaseHelper.getStakingDTOList();

    int localSize = localStakingDTOList.size();
    int remoteSize = remoteStakingDTOList.size();

    if (localStakingDTOList.toString().equals(remoteStakingDTOList.toString())) {
      LOGGER.info("[StakingDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[StakingDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localStakingDTOList, remoteStakingDTOList);
    }
  }

  /**
   * 
   */
  private void findDiff(
      @Nonnull List<? extends DatabaseDTO> localDatabaseDTOList,
      @Nonnull List<? extends DatabaseDTO> remoteDatabaseDTOList) {

    if (localDatabaseDTOList.size() == remoteDatabaseDTOList.size()) {
      for (int i = 0; i < localDatabaseDTOList.size(); i++) {
        DatabaseDTO localDatabaseDTO = localDatabaseDTOList.get(i);
        DatabaseDTO remoteDatabaseDTO = remoteDatabaseDTOList.get(i);

        if (!localDatabaseDTO.toString().equals(remoteDatabaseDTO.toString())) {
          LOGGER.error(localDatabaseDTO);
          LOGGER.error(remoteDatabaseDTO);
        }
      }
    } else {
      LOGGER.error("Size is different, time to do a manually compare!");
    }
  }
}
