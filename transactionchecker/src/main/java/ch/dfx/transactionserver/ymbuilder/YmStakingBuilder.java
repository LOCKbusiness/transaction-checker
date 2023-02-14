package ch.dfx.transactionserver.ymbuilder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class YmStakingBuilder {
  private static final Logger LOGGER = LogManager.getLogger(YmStakingBuilder.class);

  // ...
  private PreparedStatement stakingDepositSelectStatement = null;
  private PreparedStatement stakingWithdrawalSelectStatement = null;

  private PreparedStatement stakingInsertStatement = null;
  private PreparedStatement stakingUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public YmStakingBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;

    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void build(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      // ...
      List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList();

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
          int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();

          List<DepositDTO> depositDTOList = databaseBalanceHelper.getDepositDTOListByLiquidityAddressNumber(liquidityAddressNumber);
          calcStakingBalance(liquidityAddressNumber, depositDTOList);
        }
      }

      closeStatements();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      LOGGER.debug("[YmStakingBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // ...
      String stakingDepositSelectSql =
          "SELECT"
              + " ata_out.token_number,"
              + " MAX(ata_out.block_number) AS block_number,"
              + " ata_in.address_number,"
              + " SUM(ata_in.amount) AS sum_amount"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number= ata_in.type_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " ata_out.address_number=?"
              + " AND ata_in.address_number IN (SELECT deposit_address_number FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit)"
              + " GROUP BY"
              + " ata_out.token_number,"
              + " ata_in.address_number";
      stakingDepositSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingDepositSelectSql));

      String stakingWithdrawalSelectSql =
          "SELECT"
              + " ata_out.token_number,"
              + " MAX(ata_out.block_number) AS block_number,"
              + " ata_out.address_number,"
              + " SUM(ata_in.amount) AS sum_amount"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number= ata_in.type_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " ata_out.address_number IN (SELECT customer_address_number FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit)"
              + " AND ata_in.address_number=?"
              + " GROUP BY"
              + " ata_out.token_number,"
              + " ata_out.address_number";
      stakingWithdrawalSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingWithdrawalSelectSql));

      // ...
      String stakingInsertSql =
          "INSERT INTO " + TOKEN_YIELDMACHINE_SCHEMA + ".staking"
              + " (token_number, liquidity_address_number, deposit_address_number, customer_address_number"
              + ", last_in_block_number, vin"
              + ", last_out_block_number, vout)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      stakingInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingInsertSql));

      String stakingUpdateSql =
          "UPDATE " + TOKEN_YIELDMACHINE_SCHEMA + ".staking"
              + " SET last_in_block_number=?, vin=?"
              + " , last_out_block_number=?, vout=?"
              + " WHERE token_number=?"
              + " AND liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingUpdateStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingUpdateSql));
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
      stakingDepositSelectStatement.close();
      stakingWithdrawalSelectStatement.close();

      stakingInsertStatement.close();
      stakingUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void calcStakingBalance(
      int liquidityAddressNumber,
      @Nonnull List<DepositDTO> depositDTOList) throws DfxException {
    LOGGER.trace("calcStakingBalance()");

    // ...
    Map<String, StakingData> depositAddressToStakingDataMap = getDepositAddressToStakingDataMap(liquidityAddressNumber);
    Map<String, StakingData> customerAddressToStakingDataMap = getCustomerAddressToStakingDataMap(liquidityAddressNumber);

    // ...
    for (TokenEnum token : TokenEnum.values()) {
      Map<Integer, StakingDTO> depositAddressToStakingDTOMap = getDepositAddressToStakingDTOMap(token, liquidityAddressNumber);

      for (DepositDTO depositDTO : depositDTOList) {
        int depositAddressNumber = depositDTO.getDepositAddressNumber();

        String depositKey = createKey(token.getNumber(), depositAddressNumber);
        StakingData depositStakingData = depositAddressToStakingDataMap.get(depositKey);

        if (null != depositStakingData) {
          int customerAddressNumber = depositDTO.getCustomerAddressNumber();
          String customerKey = createKey(token.getNumber(), customerAddressNumber);
          StakingData customerStakingData = customerAddressToStakingDataMap.get(customerKey);

          StakingDTO stakingDTO = depositAddressToStakingDTOMap.get(depositAddressNumber);

          if (null == stakingDTO) {
            insert(token, liquidityAddressNumber, depositAddressNumber, customerAddressNumber, depositStakingData, customerStakingData);
          } else {
            update(stakingDTO, depositStakingData, customerStakingData);
          }
        }
      }
    }
  }

  /**
   * 
   */
  private Map<String, StakingData> getDepositAddressToStakingDataMap(int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("getDepositAddressToStakingDataMap()");

    try {
      Map<String, StakingData> depositAddressToStakingDataMap = new HashMap<>();

      stakingDepositSelectStatement.setInt(1, liquidityAddressNumber);

      ResultSet resultSet = stakingDepositSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingData stakingData = new StakingData();

        stakingData.tokenNumber = resultSet.getInt("token_number");
        stakingData.blockNumber = resultSet.getInt("block_number");
        stakingData.addressNumber = resultSet.getInt("address_number");
        stakingData.amount = resultSet.getBigDecimal("sum_amount");

        String key = createKey(stakingData.tokenNumber, stakingData.addressNumber);
        depositAddressToStakingDataMap.put(key, stakingData);
      }

      resultSet.close();

      return depositAddressToStakingDataMap;
    } catch (Exception e) {
      throw new DfxException("getDepositAddressToStakingDataMap", e);
    }
  }

  /**
   * 
   */
  private Map<String, StakingData> getCustomerAddressToStakingDataMap(int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("getCustomerAddressToStakingDataMap()");

    try {
      Map<String, StakingData> customerAddressToStakingDataMap = new HashMap<>();

      stakingWithdrawalSelectStatement.setInt(1, liquidityAddressNumber);

      ResultSet resultSet = stakingWithdrawalSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingData stakingData = new StakingData();

        stakingData.tokenNumber = resultSet.getInt("token_number");
        stakingData.blockNumber = resultSet.getInt("block_number");
        stakingData.addressNumber = resultSet.getInt("address_number");
        stakingData.amount = resultSet.getBigDecimal("sum_amount");

        String key = createKey(stakingData.tokenNumber, stakingData.addressNumber);
        customerAddressToStakingDataMap.put(key, stakingData);
      }

      resultSet.close();

      return customerAddressToStakingDataMap;
    } catch (Exception e) {
      throw new DfxException("getCustomerAddressToStakingDataMap", e);
    }
  }

  /**
   * 
   */
  private Map<Integer, StakingDTO> getDepositAddressToStakingDTOMap(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber) throws DfxException {
    List<StakingDTO> stakingDTOList = databaseBalanceHelper.getStakingDTOListByLiquidityAdressNumber(token, liquidityAddressNumber);

    Map<Integer, StakingDTO> depositAddressToStakingDTOMap = new HashMap<>();
    stakingDTOList.forEach(dto -> depositAddressToStakingDTOMap.put(dto.getDepositAddressNumber(), dto));

    return depositAddressToStakingDTOMap;
  }

  /**
   * 
   */
  private String createKey(int tokenNumber, int addressNumber) {
    return new StringBuilder().append(tokenNumber).append("/").append(addressNumber).toString();
  }

  /**
   * 
   */
  private void insert(
      TokenEnum token,
      int liquidityAddressNumber,
      int depositAddressNumber,
      int customerAddressNumber,
      @Nonnull StakingData depositStakingData,
      @Nullable StakingData customerStakingData) throws DfxException {
    LOGGER.trace("insert()");

    StakingDTO stakingDTO = new StakingDTO(token.getNumber(), liquidityAddressNumber, depositAddressNumber, customerAddressNumber);
    stakingDTO.setLastInBlockNumber(depositStakingData.blockNumber);
    stakingDTO.setVin(depositStakingData.amount);

    if (null == customerStakingData) {
      stakingDTO.setLastOutBlockNumber(-1);
      stakingDTO.setVout(BigDecimal.ZERO);
    } else {
      stakingDTO.setLastOutBlockNumber(customerStakingData.blockNumber);
      stakingDTO.setVout(customerStakingData.amount);
    }

    doInsert(stakingDTO);
  }

  /**
   * 
   */
  private void update(
      @Nonnull StakingDTO stakingDTO,
      @Nonnull StakingData depositStakingData,
      @Nullable StakingData customerStakingData) throws DfxException {
    LOGGER.trace("update()");

    stakingDTO.setLastInBlockNumber(depositStakingData.blockNumber);
    stakingDTO.setVin(depositStakingData.amount);

    if (null != customerStakingData) {
      stakingDTO.setLastOutBlockNumber(customerStakingData.blockNumber);
      stakingDTO.setVout(customerStakingData.amount);
    }

    if (stakingDTO.isInternalStateChanged()) {
      doUpdate(stakingDTO);
    }
  }

  /**
   * 
   */
  private void doInsert(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("doInsert()");

    try {
      int tokenNumber = stakingDTO.getTokenNumber();
      int liquidityAddressNumber = stakingDTO.getLiquidityAddressNumber();
      int depositAddressNumber = stakingDTO.getDepositAddressNumber();
      int customerAddressNumber = stakingDTO.getCustomerAddressNumber();
      int lastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int lastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      LOGGER.debug(
          "[INSERT] Token / Liquidity / Deposit / Customer: "
              + tokenNumber + " / " + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);

      stakingInsertStatement.setInt(1, tokenNumber);
      stakingInsertStatement.setInt(2, liquidityAddressNumber);
      stakingInsertStatement.setInt(3, depositAddressNumber);
      stakingInsertStatement.setInt(4, customerAddressNumber);
      stakingInsertStatement.setInt(5, lastInBlockNumber);
      stakingInsertStatement.setBigDecimal(6, stakingDTO.getVin());
      stakingInsertStatement.setInt(7, lastOutBlockNumber);
      stakingInsertStatement.setBigDecimal(8, stakingDTO.getVout());

      stakingInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("doInsert", e);
    }
  }

  /**
   * 
   */
  private void doUpdate(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("doUpdate()");

    try {
      int tokenNumber = stakingDTO.getTokenNumber();
      int liquidityAddressNumber = stakingDTO.getLiquidityAddressNumber();
      int depositAddressNumber = stakingDTO.getDepositAddressNumber();
      int customerAddressNumber = stakingDTO.getCustomerAddressNumber();
      int lastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int lastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      LOGGER.debug(
          "[UPDATE] Token / Liquidity / Deposit / Customer: "
              + tokenNumber + " / " + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);

      stakingUpdateStatement.setInt(1, lastInBlockNumber);
      stakingUpdateStatement.setBigDecimal(2, stakingDTO.getVin());
      stakingUpdateStatement.setInt(3, lastOutBlockNumber);
      stakingUpdateStatement.setBigDecimal(4, stakingDTO.getVout());

      stakingUpdateStatement.setInt(5, tokenNumber);
      stakingUpdateStatement.setInt(6, liquidityAddressNumber);
      stakingUpdateStatement.setInt(7, depositAddressNumber);
      stakingUpdateStatement.setInt(8, customerAddressNumber);

      stakingUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("doUpdate", e);
    }
  }

  /**
   * 
   */
  private class StakingData {
    private int tokenNumber = -1;
    private int blockNumber = -1;
    private int addressNumber = -1;
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * 
     */
    @Override
    public String toString() {
      return TransactionCheckerUtils.toJson(this);
    }
  }
}
