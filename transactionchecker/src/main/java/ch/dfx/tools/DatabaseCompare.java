package ch.dfx.tools;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.data.DatabaseConnectionData;
import ch.dfx.tools.data.DatabaseData;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.DatabaseDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;

/**
 * Compare table content from remote and local:
 * 
 * - MASTERNODE_WHITELIST
 * - STAKING_ADDRESS
 * - BALANCE
 * - DEPOSIT
 * - STAKING
 * 
 * Set all address numbers to "0", because of different numbers between the database systems.
 * The compare is done by the real defichain addresses, not by the database numbers.
 * 
 * But the block numbers and the transaction numbers must also be equal.
 */
public class DatabaseCompare extends DatabaseTool {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseCompare.class);

  // ...
  private final NetworkEnum network;

  /**
   * 
   */
  public DatabaseCompare(@Nonnull NetworkEnum network) {
    this.network = network;
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

      // doCompare(localConnection, remoteConnection);

      doCompare(localConnection, remoteConnection, TOKEN_STAKING_SCHEMA, TokenEnum.DFI);

      doCompare(localConnection, remoteConnection, TOKEN_YIELDMACHINE_SCHEMA, TokenEnum.DFI);
      doCompare(localConnection, remoteConnection, TOKEN_YIELDMACHINE_SCHEMA, TokenEnum.DUSD);
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

    // ...
    List<MasternodeWhitelistDTO> localMasternodeWhitelistDTOList = getLocalMasternodeWhitelistDTOList(localConnection);
    List<MasternodeWhitelistDTO> remoteMasternodeWhitelistDTOList = getRemoteMasternodeWhitelistDTOList(remoteConnection);
    compareMasternodeWhitelist(localMasternodeWhitelistDTOList, remoteMasternodeWhitelistDTOList);

//    int blockLimit = 100;
//    int blockOffset = 0;
//
//    for (int i = 0; i < 1; i++) {
//      LOGGER.debug("Block: " + blockOffset);
//
//      List<BlockDTO> localBlockDTOList = getBlockDTOList(localConnection, blockLimit, blockOffset);
//      List<BlockDTO> remoteBlockDTOList = getBlockDTOList(remoteConnection, blockLimit, blockOffset);
//      compareBlock(localBlockDTOList, remoteBlockDTOList);
//
//      blockOffset = blockOffset + blockLimit;
//    }
//
//    // ...
//    int transactionLimit = 100;
//    int transactionOffset = 0;
//
//    for (int i = 0; i < 1; i++) {
//      LOGGER.debug("Transaction: " + transactionOffset);
//
//      List<TransactionDTO> localTransactionDTOList = getTransactionDTOList(localConnection, transactionLimit, transactionOffset);
//      List<TransactionDTO> remoteTransactionDTOList = getTransactionDTOList(remoteConnection, transactionLimit, transactionOffset);
//
//      compareTransaction(localTransactionDTOList, remoteTransactionDTOList);
//
//      transactionOffset = transactionOffset + transactionLimit;
//    }
  }

  /**
   * 
   */
  private void doCompare(
      @Nonnull Connection localConnection,
      @Nonnull Connection remoteConnection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("doCompare()");

    // ...
    List<StakingAddressDTO> localStakingAddressDTOList = getLocalStakingAddressDTOList(localConnection, dbSchema, token);
    List<StakingAddressDTO> remoteStakingAddressDTOList = getRemoteStakingAddressDTOList(remoteConnection, dbSchema, token);
    compareStakingAddress(localStakingAddressDTOList, remoteStakingAddressDTOList);

    // ...
    List<BalanceDTO> localBalanceDTOList = getLocalBalanceDTOList(localConnection, dbSchema, token);
    List<BalanceDTO> remoteBalanceDTOList = getRemoteBalanceDTOList(remoteConnection, dbSchema, token);
    compareBalance(localBalanceDTOList, remoteBalanceDTOList);

    // ...
    List<DepositDTO> localDepositDTOList = getLocalDepositDTOList(localConnection, dbSchema, token);
    List<DepositDTO> remoteDepositDTOList = getRemoteDepositDTOList(remoteConnection, dbSchema, token);
    compareDeposit(localDepositDTOList, remoteDepositDTOList);

    // ...
    List<StakingDTO> localStakingDTOList = getLocalStakingDTOList(localConnection, dbSchema, token);
    List<StakingDTO> remoteStakingDTOList = getRemoteStakingDTOList(remoteConnection, dbSchema, token);
    compareStaking(localStakingDTOList, remoteStakingDTOList);
  }

  /**
   * 
   */
  private List<MasternodeWhitelistDTO> getLocalMasternodeWhitelistDTOList(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getLocalMasternodeWhitelistDTOList()");

    String masternodeSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".masternode_whitelist";
    return getMasternodeWhitelistDTOList(connection, DatabaseUtils.replaceSchema(network, masternodeSelectSql));
  }

  /**
   * 
   */
  private List<MasternodeWhitelistDTO> getRemoteMasternodeWhitelistDTOList(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getRemoteMasternodeWhitelistDTOList()");

    String masternodeSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".masternode_whitelist";
    return getMasternodeWhitelistDTOList(connection, DatabaseUtils.replaceSchema(network, masternodeSelectSql));
  }

  /**
   * 
   */
  private List<MasternodeWhitelistDTO> getMasternodeWhitelistDTOList(
      @Nonnull Connection connection,
      @Nonnull String masternodeSelectSql) throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOList()");

    try {
      List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(masternodeSelectSql);

      while (resultSet.next()) {
        MasternodeWhitelistDTO masternodeWhitelistDTO =
            new MasternodeWhitelistDTO(
                resultSet.getInt("wallet_id"),
                resultSet.getInt("idx"),
                resultSet.getString("owner_address"));

        masternodeWhitelistDTO.setTransactionId(resultSet.getString("txid"));
        masternodeWhitelistDTO.setOperatorAddress(resultSet.getString("operator_address"));
        masternodeWhitelistDTO.setRewardAddress(resultSet.getString("reward_address"));
        masternodeWhitelistDTO.setCreationBlockNumber(resultSet.getInt("creation_block_number"));
        masternodeWhitelistDTO.setResignBlockNumber(resultSet.getInt("resign_block_number"));
        masternodeWhitelistDTO.setState(resultSet.getString("state"));

        masternodeWhitelistDTOList.add(masternodeWhitelistDTO);
      }

      resultSet.close();
      statement.close();

      // ...
      Comparator<MasternodeWhitelistDTO> comparator =
          Comparator.comparing(MasternodeWhitelistDTO::getWalletId)
              .thenComparing(MasternodeWhitelistDTO::getIdx)
              .thenComparing(MasternodeWhitelistDTO::getOwnerAddress);
      masternodeWhitelistDTOList.sort(comparator);

      return masternodeWhitelistDTOList;
    } catch (Exception e) {
      throw new DfxException("getMasternodeWhitelistDTOList", e);
    }
  }

  /**
   * 
   */
  private void compareMasternodeWhitelist(
      @Nonnull List<MasternodeWhitelistDTO> localMasternodeWhitelistDTOList,
      @Nonnull List<MasternodeWhitelistDTO> remoteMasternodeWhitelistDTOList) throws DfxException {
    LOGGER.trace("compareMasternodeWhitelist()");

    int localSize = localMasternodeWhitelistDTOList.size();
    int remoteSize = remoteMasternodeWhitelistDTOList.size();

    LOGGER.debug("[MasternodeWhitelistDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    if (localMasternodeWhitelistDTOList.toString().equals(remoteMasternodeWhitelistDTOList.toString())) {
      LOGGER.debug("[MasternodeWhitelistDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[MasternodeWhitelistDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localMasternodeWhitelistDTOList, remoteMasternodeWhitelistDTOList);
    }
  }

  /**
   * 
   */
  private List<BlockDTO> getBlockDTOList(@Nonnull Connection connection, int limit, int offset) throws DfxException {
    LOGGER.trace("getBlockDTOList()");

    String blockSelectSql = "SELECT * FROM public.block ORDER BY number LIMIT " + limit + " OFFSET " + offset;

    try {
      List<BlockDTO> blockDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(blockSelectSql);

      while (resultSet.next()) {
        BlockDTO blockDTO =
            new BlockDTO(
                resultSet.getInt("number"),
                resultSet.getString("hash"),
                resultSet.getLong("timestamp"));

        blockDTOList.add(blockDTO);
      }

      resultSet.close();
      statement.close();

      return blockDTOList;
    } catch (Exception e) {
      throw new DfxException("getBlockDTOList", e);
    }
  }

  /**
   * 
   */
  private void compareBlock(
      @Nonnull List<BlockDTO> localBlockDTOList,
      @Nonnull List<BlockDTO> remoteBlockDTOList) throws DfxException {
    LOGGER.trace("compareBlock()");

    int localSize = localBlockDTOList.size();
    int remoteSize = remoteBlockDTOList.size();

    LOGGER.debug("[BlockDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    if (localBlockDTOList.toString().equals(remoteBlockDTOList.toString())) {
      LOGGER.debug("[BlockDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[BlockDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localBlockDTOList, remoteBlockDTOList);
    }
  }

  /**
   * 
   */
  private List<TransactionDTO> getTransactionDTOList(@Nonnull Connection connection, int limit, int offset) throws DfxException {
    LOGGER.trace("getTransactionDTOList()");

    String blockSelectSql = "SELECT * FROM public.transaction ORDER BY block_number, number LIMIT " + limit + " OFFSET " + offset;

    try {
      List<TransactionDTO> transactionDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(blockSelectSql);

      while (resultSet.next()) {
        TransactionDTO transactionDTO =
            new TransactionDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("number"),
                resultSet.getString("txid"));

        transactionDTOList.add(transactionDTO);
      }

      resultSet.close();
      statement.close();

      return transactionDTOList;
    } catch (Exception e) {
      throw new DfxException("getTransactionDTOList", e);
    }
  }

  /**
   * 
   */
  private void compareTransaction(
      @Nonnull List<TransactionDTO> localTransactionDTOList,
      @Nonnull List<TransactionDTO> remoteTransactionDTOList) throws DfxException {
    LOGGER.trace("compareTransaction()");

    int localSize = localTransactionDTOList.size();
    int remoteSize = remoteTransactionDTOList.size();

    LOGGER.debug("[TransactionDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    if (localTransactionDTOList.toString().equals(remoteTransactionDTOList.toString())) {
      LOGGER.debug("[TransactionDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[TransactionDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localTransactionDTOList, remoteTransactionDTOList);
    }
  }

  /**
   * 
   */
  private List<StakingAddressDTO> getLocalStakingAddressDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getLocalStakingAddressDTOList()");

    String stakingAddressSelectSql =
        "SELECT"
            + " s.*,"
            + " a1.address AS liquidity_address,"
            + " a2.address AS reward_address,"
            + " FROM " + dbSchema + ".staking_address s"
            + " JOIN public.address a1 ON"
            + " s.liquidity_address_number = a1.number"
            + " LEFT JOIN public.address a2 ON"
            + " s.reward_address_number = a2.number"
            + " WHERE token_number=" + token.getNumber();
    return getStakingAddressDTOList(connection, DatabaseUtils.replaceSchema(network, stakingAddressSelectSql));
  }

  /**
   * 
   */
  private List<StakingAddressDTO> getRemoteStakingAddressDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getRemoteStakingAddressDTOList()");

    String stakingAddressSelectSql =
        "SELECT"
            + " s.*,"
            + " a1.address AS liquidity_address,"
            + " a2.address AS reward_address,"
            + " FROM " + dbSchema + ".staking_address s"
            + " JOIN public.address a1 ON"
            + " s.liquidity_address_number = a1.number"
            + " LEFT JOIN public.address a2 ON"
            + " s.reward_address_number = a2.number"
            + " WHERE token_number=" + token.getNumber();
    return getStakingAddressDTOList(connection, DatabaseUtils.replaceSchema(network, stakingAddressSelectSql));
  }

  /**
   * 
   */
  private List<StakingAddressDTO> getStakingAddressDTOList(
      @Nonnull Connection connection,
      @Nonnull String stakingAddressSelectSql) throws DfxException {
    LOGGER.trace("getStakingAddressList()");

    try {
      List<StakingAddressDTO> stakingAddressDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(stakingAddressSelectSql);

      while (resultSet.next()) {
        StakingAddressDTO stakingAddressDTO = new StakingAddressDTO(resultSet.getInt("token_number"));

        stakingAddressDTO.setLiquidityAddressNumber(resultSet.getInt("liquidity_address_number"));
        stakingAddressDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));

        stakingAddressDTO.setRewardAddressNumber(resultSet.getInt("reward_address_number"));
        stakingAddressDTO.setRewardAddress(resultSet.getString("reward_address"));

        stakingAddressDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        stakingAddressDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        stakingAddressDTOList.add(stakingAddressDTO);
      }

      resultSet.close();
      statement.close();

      // ...
      Comparator<StakingAddressDTO> comparator =
          Comparator.comparing(StakingAddressDTO::getLiquidityAddress);
      stakingAddressDTOList.sort(comparator);

      return stakingAddressDTOList;
    } catch (Exception e) {
      throw new DfxException("getStakingAddressList", e);
    }
  }

  /**
   * 
   */
  private void compareStakingAddress(
      @Nonnull List<StakingAddressDTO> localStakingAddressDTOList,
      @Nonnull List<StakingAddressDTO> remoteStakingAddressDTOList) throws DfxException {
    LOGGER.trace("compareStakingAddress()");

    int localSize = localStakingAddressDTOList.size();
    int remoteSize = remoteStakingAddressDTOList.size();

    LOGGER.debug("[StakingAddressDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    if (localStakingAddressDTOList.toString().equals(remoteStakingAddressDTOList.toString())) {
      LOGGER.debug("[StakingAddressDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[StakingAddressDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localStakingAddressDTOList, remoteStakingAddressDTOList);
    }
  }

  /**
   * 
   */
  private List<BalanceDTO> getLocalBalanceDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getLocalBalanceDTOList()");

    String balanceSelectSql =
        "SELECT"
            + " b.*,"
            + " a.address"
            + " FROM " + dbSchema + ".balance b"
            + " JOIN public.address a ON"
            + " b.address_number = a.number"
            + " WHERE b.token_number=" + token.getNumber();
    return getBalanceDTOList(connection, DatabaseUtils.replaceSchema(network, balanceSelectSql));
  }

  /**
   * 
   */
  private List<BalanceDTO> getRemoteBalanceDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getRemoteBalanceDTOList()");

    String balanceSelectSql =
        "SELECT"
            + " b.*,"
            + " a.address"
            + " FROM " + dbSchema + ".balance b"
            + " JOIN public.address a ON"
            + " b.address_number = a.number"
            + " WHERE b.token_number=" + token.getNumber();
    return getBalanceDTOList(connection, DatabaseUtils.replaceSchema(network, balanceSelectSql));
  }

  /**
   * 
   */
  private List<BalanceDTO> getBalanceDTOList(
      @Nonnull Connection connection,
      @Nonnull String balanceSelectSql) throws DfxException {
    LOGGER.trace("getBalanceDTOList()");

    try {
      List<BalanceDTO> balanceDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(balanceSelectSql);

      while (resultSet.next()) {
        BalanceDTO balanceDTO =
            new BalanceDTO(
                resultSet.getInt("token_number"),
                0);
        balanceDTO.setAddress(resultSet.getString("address"));

        balanceDTO.setBlockNumber(resultSet.getInt("block_number"));
        balanceDTO.setTransactionCount(resultSet.getInt("transaction_count"));

        balanceDTO.setVout(resultSet.getBigDecimal("vout"));
        balanceDTO.setVin(resultSet.getBigDecimal("vin"));

        balanceDTOList.add(balanceDTO);
      }

      resultSet.close();
      statement.close();

      // ...
      Comparator<BalanceDTO> comparator =
          Comparator.comparing(BalanceDTO::getAddress);
      balanceDTOList.sort(comparator);

      return balanceDTOList;
    } catch (Exception e) {
      throw new DfxException("getBalanceDTOList", e);
    }
  }

  /**
   * 
   */
  private void compareBalance(
      @Nonnull List<BalanceDTO> localBalanceDTOList,
      @Nonnull List<BalanceDTO> remoteBalanceDTOList) throws DfxException {
    LOGGER.trace("compareBalance()");

    int localSize = localBalanceDTOList.size();
    int remoteSize = remoteBalanceDTOList.size();

    LOGGER.debug("[BalanceDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    if (localBalanceDTOList.toString().equals(remoteBalanceDTOList.toString())) {
      LOGGER.debug("[BalanceDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[BalanceDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localBalanceDTOList, remoteBalanceDTOList);
    }
  }

  /**
   * 
   */
  private List<DepositDTO> getLocalDepositDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getLocalDepositDTOList()");

    String depositSelectSql =
        "SELECT"
            + " d.*,"
            + " a1.address AS liquidity_address,"
            + " a2.address AS deposit_address,"
            + " a3.address AS customer_address"
            + " FROM " + dbSchema + ".deposit d"
            + " JOIN public.address a1 ON"
            + " d.liquidity_address_number = a1.number"
            + " JOIN public.address a2 ON"
            + " d.deposit_address_number = a2.number"
            + " JOIN public.address a3 ON"
            + " d.customer_address_number = a3.number"
            + " WHERE d.token_number=" + token.getNumber();
    return getDepositDTOList(connection, DatabaseUtils.replaceSchema(network, depositSelectSql));
  }

  /**
   * 
   */
  private List<DepositDTO> getRemoteDepositDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getRemoteDepositDTOList()");

    String depositSelectSql =
        "SELECT"
            + " d.*,"
            + " a1.address AS liquidity_address,"
            + " a2.address AS deposit_address,"
            + " a3.address AS customer_address"
            + " FROM " + dbSchema + ".deposit d"
            + " JOIN public.address a1 ON"
            + " d.liquidity_address_number = a1.number"
            + " JOIN public.address a2 ON"
            + " d.deposit_address_number = a2.number"
            + " JOIN public.address a3 ON"
            + " d.customer_address_number = a3.number"
            + " WHERE d.token_number=" + token.getNumber();
    return getDepositDTOList(connection, DatabaseUtils.replaceSchema(network, depositSelectSql));
  }

  /**
   * 
   */
  private List<DepositDTO> getDepositDTOList(
      @Nonnull Connection connection,
      @Nonnull String depositSelectSql) throws DfxException {
    LOGGER.trace("getDepositDTOList()");

    try {
      List<DepositDTO> depositDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(depositSelectSql);

      while (resultSet.next()) {
        DepositDTO depositDTO = new DepositDTO(resultSet.getInt("token_number"));

        depositDTO.setLiquidityAddressNumber(0);
        depositDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));
        depositDTO.setDepositAddressNumber(0);
        depositDTO.setDepositAddress(resultSet.getString("deposit_address"));
        depositDTO.setCustomerAddressNumber(0);
        depositDTO.setCustomerAddress(resultSet.getString("customer_address"));

        depositDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        depositDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        depositDTOList.add(depositDTO);
      }

      resultSet.close();
      statement.close();

      // ...
      Comparator<DepositDTO> comparator =
          Comparator.comparing(DepositDTO::getLiquidityAddress)
              .thenComparing(DepositDTO::getDepositAddress)
              .thenComparing(DepositDTO::getCustomerAddress);
      depositDTOList.sort(comparator);

      return depositDTOList;
    } catch (Exception e) {
      throw new DfxException("getDepositDTOList", e);
    }
  }

  /**
   * 
   */
  private void compareDeposit(
      @Nonnull List<DepositDTO> localDepositDTOList,
      @Nonnull List<DepositDTO> remoteDepositDTOList) throws DfxException {
    LOGGER.trace("compareDeposit()");

    int localSize = localDepositDTOList.size();
    int remoteSize = remoteDepositDTOList.size();

    LOGGER.debug("[DepositDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    if (localDepositDTOList.toString().equals(remoteDepositDTOList.toString())) {
      LOGGER.debug("[DepositDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
    } else {
      LOGGER.error("[DepositDTO]: local(" + localSize + ") not equals remote(" + remoteSize + ")");
      findDiff(localDepositDTOList, remoteDepositDTOList);
    }
  }

  /**
   * 
   */
  private List<StakingDTO> getLocalStakingDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getLocalStakingDTOList()");

    String stakingSelectSql =
        "SELECT"
            + " s.*,"
            + " a1.address AS liquidity_address,"
            + " a2.address AS deposit_address,"
            + " a3.address AS customer_address"
            + " FROM " + dbSchema + ".staking s"
            + " JOIN public.address a1 ON"
            + " s.liquidity_address_number = a1.number"
            + " JOIN public.address a2 ON"
            + " s.deposit_address_number = a2.number"
            + " JOIN public.address a3 ON"
            + " s.customer_address_number = a3.number"
            + " WHERE s.token_number=" + token.getNumber();
    return getStakingDTOList(connection, DatabaseUtils.replaceSchema(network, stakingSelectSql));
  }

  /**
   * 
   */
  private List<StakingDTO> getRemoteStakingDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getRemoteStakingDTOList()");

    String stakingSelectSql =
        "SELECT"
            + " s.*,"
            + " a1.address AS liquidity_address,"
            + " a2.address AS deposit_address,"
            + " a3.address AS customer_address"
            + " FROM " + dbSchema + ".staking s"
            + " JOIN public.address a1 ON"
            + " s.liquidity_address_number = a1.number"
            + " JOIN public.address a2 ON"
            + " s.deposit_address_number = a2.number"
            + " JOIN public.address a3 ON"
            + " s.customer_address_number = a3.number"
            + " WHERE s.token_number=" + token.getNumber();
    return getStakingDTOList(connection, DatabaseUtils.replaceSchema(network, stakingSelectSql));
  }

  /**
   * 
   */
  private List<StakingDTO> getStakingDTOList(
      @Nonnull Connection connection,
      @Nonnull String stakingSelectSql) throws DfxException {
    LOGGER.trace("getStakingDTOList()");

    try {
      List<StakingDTO> stakingDTOList = new ArrayList<>();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(stakingSelectSql);

      while (resultSet.next()) {
        StakingDTO stakingDTO =
            new StakingDTO(
                resultSet.getInt("token_number"),
                0,
                0,
                0);

        stakingDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));
        stakingDTO.setDepositAddress(resultSet.getString("deposit_address"));
        stakingDTO.setCustomerAddress(resultSet.getString("customer_address"));

        stakingDTO.setLastInBlockNumber(resultSet.getInt("last_in_block_number"));
        stakingDTO.setVin(resultSet.getBigDecimal("vin"));
        stakingDTO.setLastOutBlockNumber(resultSet.getInt("last_out_block_number"));
        stakingDTO.setVout(resultSet.getBigDecimal("vout"));

        stakingDTOList.add(stakingDTO);
      }

      resultSet.close();
      statement.close();

      // ...
      Comparator<StakingDTO> comparator =
          Comparator.comparing(StakingDTO::getLiquidityAddress)
              .thenComparing(StakingDTO::getDepositAddress)
              .thenComparing(StakingDTO::getCustomerAddress);
      stakingDTOList.sort(comparator);

      return stakingDTOList;
    } catch (Exception e) {
      throw new DfxException("getStakingDTOList", e);
    }
  }

  /**
   * 
   */
  private void compareStaking(
      @Nonnull List<StakingDTO> localStakingDTOList,
      @Nonnull List<StakingDTO> remoteStakingDTOList) throws DfxException {
    LOGGER.trace("compareStaking()");

    int localSize = localStakingDTOList.size();
    int remoteSize = remoteStakingDTOList.size();

    LOGGER.debug("[StakingDTO]: local(" + localSize + ") compare to remote(" + remoteSize + ")");

    // ...

    if (localStakingDTOList.toString().equals(remoteStakingDTOList.toString())) {
      LOGGER.debug("[StakingDTO]: local(" + localSize + ") equals remote(" + remoteSize + ")");
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

        findDiff(localDatabaseDTO, remoteDatabaseDTO);
      }
    } else {
      LOGGER.error("Size is different, time to do a manually compare!");
    }
  }

  /**
   * 
   */
  private void findDiff(
      @Nonnull DatabaseDTO localDatabaseDTO,
      @Nonnull DatabaseDTO remoteDatabaseDTO) {
    if (!localDatabaseDTO.toString().equals(remoteDatabaseDTO.toString())) {
      LOGGER.error("LOCAL:\n" + localDatabaseDTO);
      LOGGER.error("REMOTE:\n" + remoteDatabaseDTO);
    }
  }
}
