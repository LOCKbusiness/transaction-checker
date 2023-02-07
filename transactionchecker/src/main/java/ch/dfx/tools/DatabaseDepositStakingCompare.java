package ch.dfx.tools;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.data.DatabaseConnectionData;
import ch.dfx.tools.data.DatabaseData;
import ch.dfx.transactionserver.data.DatabaseDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;

/**
 * 
 */
public class DatabaseDepositStakingCompare extends DatabaseTool {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseDepositStakingCompare.class);

  // ...
  private final NetworkEnum network;

  /**
   * 
   */
  public DatabaseDepositStakingCompare(@Nonnull NetworkEnum network) {
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

    // ...
    List<DepositDTO> localDepositDTOList = getLocalDepositDTOList(localConnection, TOKEN_YIELDMACHINE_SCHEMA);
    List<DepositDTO> remoteDepositDTOList = getRemoteDepositDTOList(remoteConnection, TOKEN_NETWORK_SCHEMA);
    compareDeposit(localDepositDTOList, remoteDepositDTOList);
  }

  /**
   * 
   */
  private List<DepositDTO> getLocalDepositDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema) throws DfxException {
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
            + " WHERE d.token_number = 15";
    return getDepositDTOList(connection, DatabaseUtils.replaceSchema(network, depositSelectSql));
  }

  /**
   * 
   */
  private List<DepositDTO> getRemoteDepositDTOList(
      @Nonnull Connection connection,
      @Nonnull String dbSchema) throws DfxException {
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
            + " WHERE d.token_number = 15";
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

    Map<String, DepositDTO> localDepositDTOMap = new HashMap<>();
    Map<String, DepositDTO> remoteDepositDTOMap = new HashMap<>();

    localDepositDTOList.forEach(dto -> localDepositDTOMap.put(dto.getDepositAddress(), dto));
    remoteDepositDTOList.forEach(dto -> remoteDepositDTOMap.put(dto.getDepositAddress(), dto));

    // ...
    for (Entry<String, DepositDTO> localDepositDTOMapEntry : localDepositDTOMap.entrySet()) {
      String depositAddress = localDepositDTOMapEntry.getKey();
      DepositDTO localDepositDTO = localDepositDTOMapEntry.getValue();
      DepositDTO remoteDepositDTO = remoteDepositDTOMap.get(depositAddress);

      if (null == remoteDepositDTO) {
        LOGGER.error("Remote Deposit DTO not found: " + localDepositDTO);
      } else {
        localDepositDTO.setStartBlockNumber(0);
        localDepositDTO.setStartTransactionNumber(0);
        remoteDepositDTO.setStartBlockNumber(0);
        remoteDepositDTO.setStartTransactionNumber(0);

        findDiff(localDepositDTO, remoteDepositDTO);
      }
    }

    // ...
    for (Entry<String, DepositDTO> remoteDepositDTOMapEntry : remoteDepositDTOMap.entrySet()) {
      String depositAddress = remoteDepositDTOMapEntry.getKey();
      DepositDTO remoteDepositDTO = remoteDepositDTOMapEntry.getValue();
      DepositDTO localDepositDTO = localDepositDTOMap.get(depositAddress);

      if (null == localDepositDTO) {
        LOGGER.error("Local Deposit DTO not found: " + remoteDepositDTO);
      } else {
        localDepositDTO.setStartBlockNumber(0);
        localDepositDTO.setStartTransactionNumber(0);
        remoteDepositDTO.setStartBlockNumber(0);
        remoteDepositDTO.setStartTransactionNumber(0);

        findDiff(remoteDepositDTO, localDepositDTO);
      }
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
