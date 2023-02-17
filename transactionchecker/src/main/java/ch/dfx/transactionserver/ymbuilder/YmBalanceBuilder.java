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

import ch.dfx.TransactionCheckerUtils;
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
  private PreparedStatement inAmountSelectStatement = null;
  private PreparedStatement outAmountSelectStatement = null;

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
  public void build(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList();

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
          int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();

          List<DepositDTO> depositDTOList = databaseBalanceHelper.getDepositDTOListByLiquidityAddressNumber(liquidityAddressNumber);
          calcDepositBalance(depositDTOList);
        }
      }

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
      // In ...
      String inAmountSelectSql =
          "SELECT"
              + " token_number,"
              + " MAX(block_number) AS block_number,"
              + " address_number,"
              + " SUM(amount) AS sum_amount,"
              + " COUNT(amount) AS count"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in"
              + " WHERE"
              + " address_number IN (SELECT deposit_address_number FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit)"
              + " GROUP BY"
              + " token_number,"
              + " address_number";
      inAmountSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, inAmountSelectSql));

      // Out ...
      String outAmountSelectSql =
          "SELECT"
              + " token_number,"
              + " MAX(block_number) AS block_number,"
              + " address_number,"
              + " SUM(amount) AS sum_amount,"
              + " COUNT(amount) AS count"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out"
              + " WHERE"
              + " address_number IN (SELECT deposit_address_number FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit)"
              + " GROUP BY"
              + " token_number,"
              + " address_number";
      outAmountSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, outAmountSelectSql));

      // Balance ...
      String balanceInsertSql =
          "INSERT INTO " + TOKEN_YIELDMACHINE_SCHEMA + ".balance"
              + " (token_number, address_number, block_number, transaction_count, vout, vin)"
              + " VALUES(?, ?, ?, ?, ?, ?)";
      balanceInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, balanceInsertSql));

      String balanceUpdateSql =
          "UPDATE " + TOKEN_YIELDMACHINE_SCHEMA + ".balance"
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
      inAmountSelectStatement.close();
      outAmountSelectStatement.close();

      balanceInsertStatement.close();
      balanceUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void calcDepositBalance(@Nonnull List<DepositDTO> depositDTOList) throws DfxException {
    LOGGER.trace("calcDepositBalance()");

    Map<String, BalanceData> inDepositAddressToBalanceDataMap = getInDepositAddressToBalanceDataMap();
    Map<String, BalanceData> outDepositAddressToBalanceDataMap = getOutDepositAddressToBalanceDataMap();

    // ...
    for (TokenEnum token : TokenEnum.values()) {
      Map<Integer, BalanceDTO> depositAddressToBalanceDTOMap = getDepositAddressToBalanceDTOMap(token);

      for (DepositDTO depositDTO : depositDTOList) {
        int depositAddressNumber = depositDTO.getDepositAddressNumber();

        String depositKey = createKey(token.getNumber(), depositAddressNumber);
        BalanceData inBalanceData = inDepositAddressToBalanceDataMap.get(depositKey);

        if (null != inBalanceData) {
          BalanceData outBalanceData = outDepositAddressToBalanceDataMap.get(depositKey);

          BalanceDTO balanceDTO = depositAddressToBalanceDTOMap.get(depositAddressNumber);

          if (null == balanceDTO) {
            insert(token, depositAddressNumber, inBalanceData, outBalanceData);
          } else {
            update(balanceDTO, inBalanceData, outBalanceData);
          }
        }
      }
    }
  }

  /**
   * 
   */
  private Map<String, BalanceData> getInDepositAddressToBalanceDataMap() throws DfxException {
    LOGGER.trace("getDepositAddressToBalanceDataMap()");

    try {
      Map<String, BalanceData> depositAddressToBalanceDataMap = new HashMap<>();

      ResultSet resultSet = inAmountSelectStatement.executeQuery();

      while (resultSet.next()) {
        BalanceData balanceData = new BalanceData();

        balanceData.tokenNumber = resultSet.getInt("token_number");
        balanceData.blockNumber = resultSet.getInt("block_number");
        balanceData.addressNumber = resultSet.getInt("address_number");
        balanceData.amount = resultSet.getBigDecimal("sum_amount");
        balanceData.count = resultSet.getInt("count");

        String key = createKey(balanceData.tokenNumber, balanceData.addressNumber);
        depositAddressToBalanceDataMap.put(key, balanceData);
      }

      resultSet.close();

      return depositAddressToBalanceDataMap;
    } catch (Exception e) {
      throw new DfxException("getDepositAddressToBalanceDataMap", e);
    }
  }

  /**
   * 
   */
  private Map<String, BalanceData> getOutDepositAddressToBalanceDataMap() throws DfxException {
    LOGGER.trace("getDepositAddressToBalanceDataMap()");

    try {
      Map<String, BalanceData> depositAddressToBalanceDataMap = new HashMap<>();

      ResultSet resultSet = outAmountSelectStatement.executeQuery();

      while (resultSet.next()) {
        BalanceData balanceData = new BalanceData();

        balanceData.tokenNumber = resultSet.getInt("token_number");
        balanceData.blockNumber = resultSet.getInt("block_number");
        balanceData.addressNumber = resultSet.getInt("address_number");
        balanceData.amount = resultSet.getBigDecimal("sum_amount");
        balanceData.count = resultSet.getInt("count");

        String key = createKey(balanceData.tokenNumber, balanceData.addressNumber);
        depositAddressToBalanceDataMap.put(key, balanceData);
      }

      resultSet.close();

      return depositAddressToBalanceDataMap;
    } catch (Exception e) {
      throw new DfxException("getDepositAddressToBalanceDataMap", e);
    }
  }

  /**
   * 
   */
  private Map<Integer, BalanceDTO> getDepositAddressToBalanceDTOMap(@Nonnull TokenEnum token) throws DfxException {
    List<BalanceDTO> balanceDTOList = databaseBalanceHelper.getBalanceDTOList(token);

    Map<Integer, BalanceDTO> depositAddressToBalanceDTOMap = new HashMap<>();
    balanceDTOList.forEach(dto -> depositAddressToBalanceDTOMap.put(dto.getAddressNumber(), dto));

    return depositAddressToBalanceDTOMap;
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
      int depositAddressNumber,
      @Nonnull BalanceData inBalanceData,
      @Nullable BalanceData outBalanceData) throws DfxException {
    LOGGER.trace("insert()");

    BalanceDTO balanceBTO = new BalanceDTO(token.getNumber(), depositAddressNumber);
    balanceBTO.setBlockNumber(inBalanceData.blockNumber);
    balanceBTO.setVin(inBalanceData.amount);

    int count = inBalanceData.count;

    if (null == outBalanceData) {
      balanceBTO.setVout(BigDecimal.ZERO);
    } else {
      count += outBalanceData.count;
      balanceBTO.setVout(outBalanceData.amount);
    }

    balanceBTO.setTransactionCount(count);

    doInsert(balanceBTO);
  }

  /**
   * 
   */
  private void update(
      @Nonnull BalanceDTO balanceDTO,
      @Nonnull BalanceData inBalanceData,
      @Nullable BalanceData outBalanceData) throws DfxException {
    LOGGER.trace("update()");

    balanceDTO.setBlockNumber(inBalanceData.blockNumber);
    balanceDTO.setVin(inBalanceData.amount);

    int count = inBalanceData.count;

    if (null != outBalanceData) {
      count += outBalanceData.count;
      balanceDTO.setVout(outBalanceData.amount);
    }

    balanceDTO.setTransactionCount(count);

    if (balanceDTO.isInternalStateChanged()) {
      doUpdate(balanceDTO);
    }
  }

  /**
   * 
   */
  private void doInsert(@Nonnull BalanceDTO balanceDTO) throws DfxException {
    LOGGER.trace("doInsert()");

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
      throw new DfxException("doInsert", e);
    }
  }

  /**
   * 
   */
  private void doUpdate(@Nonnull BalanceDTO balanceDTO) throws DfxException {
    LOGGER.trace("doUpdate()");

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
      throw new DfxException("doUpdate", e);
    }
  }

  /**
   * 
   */
  private class BalanceData {
    private int tokenNumber = -1;
    private int blockNumber = -1;
    private int addressNumber = -1;
    private BigDecimal amount = BigDecimal.ZERO;
    private int count = 0;

    /**
     * 
     */
    @Override
    public String toString() {
      return TransactionCheckerUtils.toJson(this);
    }
  }
}
