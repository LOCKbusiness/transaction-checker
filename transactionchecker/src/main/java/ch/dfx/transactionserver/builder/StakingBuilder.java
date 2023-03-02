package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
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
public class StakingBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StakingBuilder.class);

  // ...
  private PreparedStatement stakingVinSelectStatement = null;
  private PreparedStatement customStakingVinSelectStatement = null;
  private PreparedStatement stakingVoutSelectStatement = null;

  private PreparedStatement stakingSelectStatement = null;
  private PreparedStatement stakingInsertStatement = null;
  private PreparedStatement stakingUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StakingBuilder(
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
    LOGGER.debug("build(): token=" + token);

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      calcStakingBalance(connection, token);

      closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      LOGGER.debug("[StakingBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String stakingVinSelectSql =
          "WITH X AS ("
              + "SELECT"
              + " at_in.in_block_number,"
              + " at_in.address_number,"
              + " at_in.vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction t"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " t.block_number = at_out.block_number"
              + " AND t.number = at_out.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " t.custom_type_code='0'"
              + " AND at_out.address_number=?"
              + " AND at_in.address_number IN"
              + " (SELECT deposit_address_number FROM " + TOKEN_STAKING_SCHEMA + ".deposit"
              + " WHERE token_number=? AND liquidity_address_number=?)"
              + " GROUP BY"
              + " at_out.block_number,"
              + " at_in.in_block_number,"
              + " at_in.in_transaction_number,"
              + " at_in.address_number,"
              + " at_in.vin"
              + ")"
              + " SELECT"
              + " MAX(in_block_number) AS in_block_number,"
              + " address_number,"
              + " SUM(vin) AS vin"
              + " FROM X"
              + " GROUP BY"
              + " address_number";

//      String stakingVinSelectSql =
//          "WITH X AS ("
//              + "SELECT"
//              + " at_in.in_block_number,"
//              + " at_in.address_number,"
//              + " at_in.vin"
//              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out"
//              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
//              + " at_out.block_number = at_in.block_number"
//              + " AND at_out.transaction_number = at_in.transaction_number"
//              + " WHERE"
//              + " at_out.address_number=?"
//              + " AND at_in.address_number IN"
//              + " (SELECT deposit_address_number FROM " + TOKEN_STAKING_SCHEMA + ".deposit"
//              + " WHERE token_number=? AND liquidity_address_number=?)"
//              + " GROUP BY"
//              + " at_out.block_number,"
//              + " at_in.in_block_number,"
//              + " at_in.in_transaction_number,"
//              + " at_in.address_number,"
//              + " at_in.vin"
//              + ")"
//              + "SELECT"
//              + " MAX(in_block_number) AS in_block_number,"
//              + " address_number,"
//              + " SUM(vin) AS vin"
//              + " FROM X"
//              + " GROUP BY"
//              + " address_number";

      stakingVinSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingVinSelectSql));

      // ...
      String customStakingVinSelectSql =
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
              + " ata_out.token_number=0"
              + " AND ata_out.address_number=?"
              + " AND ata_in.address_number IN (SELECT deposit_address_number FROM " + TOKEN_STAKING_SCHEMA + ".deposit)"
              + " GROUP BY"
              + " ata_out.token_number,"
              + " ata_in.address_number";
      customStakingVinSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, customStakingVinSelectSql));

      // ...
      String stakingVoutSelectSql =
          "WITH X AS ("
              + "SELECT"
              + " at_out.block_number,"
              + " at_out.address_number,"
              + " at_out.vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " at_in.address_number=?"
              + " AND at_out.address_number IN"
              + " (SELECT customer_address_number FROM " + TOKEN_STAKING_SCHEMA + ".deposit"
              + " WHERE token_number=? AND liquidity_address_number=?)"
              + " GROUP BY"
              + " at_out.block_number,"
              + " at_out.transaction_number,"
              + " at_out.address_number,"
              + " at_out.vout"
              + ")"
              + " SELECT"
              + " MAX(block_number) AS block_number,"
              + " address_number,"
              + " SUM(vout) AS vout"
              + " FROM X"
              + " GROUP BY"
              + " address_number";
      stakingVoutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingVoutSelectSql));

      String stakingSelectSql =
          "SELECT * FROM " + TOKEN_STAKING_SCHEMA + ".staking"
              + " WHERE token_number=?"
              + " AND liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingSelectSql));

      String stakingInsertSql =
          "INSERT INTO " + TOKEN_STAKING_SCHEMA + ".staking"
              + " (token_number, liquidity_address_number, deposit_address_number, customer_address_number"
              + ", last_in_block_number, vin"
              + ", last_out_block_number, vout)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      stakingInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingInsertSql));

      String stakingUpdateSql =
          "UPDATE " + TOKEN_STAKING_SCHEMA + ".staking"
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
      stakingVinSelectStatement.close();
      customStakingVinSelectStatement.close();
      stakingVoutSelectStatement.close();

      stakingSelectStatement.close();
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
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("calcStakingBalance()");

    List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList();

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        calcBalance(connection, token, stakingAddressDTO);
      }
    }
  }

  /**
   * 
   */
  private void calcBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull StakingAddressDTO stakingAddressDTO) throws DfxException {
    LOGGER.trace("calcBalance()");

    try {
      int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();

      // ...
      List<StakingDTO> storedStakingDTOList =
          databaseBalanceHelper.getStakingDTOListByLiquidityAdressNumber(token, liquidityAddressNumber);
      List<StakingDTO> newStakingDTOList = new ArrayList<>();

      Map<Integer, StakingDTO> storedDepositToStakingDTOMap = new HashMap<>();
      storedStakingDTOList.forEach(dto -> storedDepositToStakingDTOMap.put(dto.getDepositAddressNumber(), dto));

      // ...
      Map<Integer, StakingDTO> depositToStakingDTOMap = calcVin(token, liquidityAddressNumber);
      Map<Integer, StakingDTO> customerToStakingDTOMap = calcVout(token, liquidityAddressNumber);

      // ...
      mergeDepositCustomTransaction(liquidityAddressNumber, depositToStakingDTOMap);

      // ...
      List<DepositDTO> depositDTOList =
          databaseBalanceHelper.getDepositDTOListByLiquidityAddressNumber(liquidityAddressNumber);

      for (DepositDTO depositDTO : depositDTOList) {
        int depositAddressNumber = depositDTO.getDepositAddressNumber();
        int customerAddressNumber = depositDTO.getCustomerAddressNumber();

        StakingDTO storedDepositStakingDTO = storedDepositToStakingDTOMap.get(depositAddressNumber);

        if (null == storedDepositStakingDTO) {
          storedDepositStakingDTO =
              new StakingDTO(
                  token.getNumber(),
                  liquidityAddressNumber,
                  depositAddressNumber,
                  customerAddressNumber);

          newStakingDTOList.add(storedDepositStakingDTO);
        }

        StakingDTO depositStakingDTO = depositToStakingDTOMap.get(depositAddressNumber);
        StakingDTO customerStakingDTO = customerToStakingDTOMap.get(customerAddressNumber);

        if (null != depositStakingDTO) {
          storedDepositStakingDTO.setLastInBlockNumber(depositStakingDTO.getLastInBlockNumber());
          storedDepositStakingDTO.setVin(depositStakingDTO.getVin());
        }

        if (null != customerStakingDTO) {
          storedDepositStakingDTO.setLastOutBlockNumber(customerStakingDTO.getLastOutBlockNumber());
          storedDepositStakingDTO.setVout(customerStakingDTO.getVout());
        }
      }

      // ...
      for (StakingDTO newStakingDTO : newStakingDTOList) {
        insertStaking(newStakingDTO);
      }

      for (StakingDTO storedStakingDTO : storedStakingDTOList) {
        if (storedStakingDTO.isInternalStateChanged()) {
          updateStaking(storedStakingDTO);
        }
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
  private Map<Integer, StakingDTO> calcVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("calcVin()");

    try {
      Map<Integer, StakingDTO> depositToStakingDTOMap = new HashMap<>();

      stakingVinSelectStatement.setInt(1, liquidityAddressNumber);
      stakingVinSelectStatement.setInt(2, token.getNumber());
      stakingVinSelectStatement.setInt(3, liquidityAddressNumber);

      ResultSet resultSet = stakingVinSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingDTO stakingDTO =
            new StakingDTO(token.getNumber(), liquidityAddressNumber, resultSet.getInt("address_number"), -1);

        stakingDTO.setLastInBlockNumber(resultSet.getInt("in_block_number"));
        stakingDTO.setVin(resultSet.getBigDecimal("vin"));

        depositToStakingDTOMap.put(stakingDTO.getDepositAddressNumber(), stakingDTO);
      }

      resultSet.close();

      return depositToStakingDTOMap;
    } catch (Exception e) {
      throw new DfxException("calcVin", e);
    }
  }

  /**
   * 
   */
  private Map<Integer, StakingDTO> calcVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("calcVout()");

    try {
      Map<Integer, StakingDTO> customerToStakingDTOMap = new HashMap<>();

      stakingVoutSelectStatement.setInt(1, liquidityAddressNumber);
      stakingVoutSelectStatement.setInt(2, token.getNumber());
      stakingVoutSelectStatement.setInt(3, liquidityAddressNumber);

      ResultSet resultSet = stakingVoutSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingDTO stakingDTO =
            new StakingDTO(token.getNumber(), liquidityAddressNumber, -1, resultSet.getInt("address_number"));

        stakingDTO.setLastOutBlockNumber(resultSet.getInt("block_number"));
        stakingDTO.setVout(resultSet.getBigDecimal("vout"));

        customerToStakingDTOMap.put(stakingDTO.getCustomerAddressNumber(), stakingDTO);
      }

      resultSet.close();

      return customerToStakingDTOMap;
    } catch (Exception e) {
      throw new DfxException("calcVout", e);
    }
  }

  /**
   * 
   */
  private void mergeDepositCustomTransaction(
      int liquidityAddressNumber,
      @Nonnull Map<Integer, StakingDTO> depositToStakingDTOMap) throws DfxException {
    Map<Integer, CustomStakingData> depositAddressToCustomStakingDataMap = getDepositAddressToCustomStakingDataMap(liquidityAddressNumber);

    for (Entry<Integer, CustomStakingData> depositAddressToCustomStakingDataMapEntry : depositAddressToCustomStakingDataMap.entrySet()) {
      Integer depositAddressNumber = depositAddressToCustomStakingDataMapEntry.getKey();
      CustomStakingData customStakingData = depositAddressToCustomStakingDataMapEntry.getValue();

      StakingDTO stakingDTO = depositToStakingDTOMap.get(depositAddressNumber);

      if (null == stakingDTO) {
        stakingDTO =
            new StakingDTO(customStakingData.tokenNumber, liquidityAddressNumber, depositAddressNumber, -1);

        stakingDTO.setLastInBlockNumber(customStakingData.blockNumber);
        stakingDTO.setVin(customStakingData.amount);

        depositToStakingDTOMap.put(depositAddressNumber, stakingDTO);
      } else {
        stakingDTO.setLastInBlockNumber(Math.max(stakingDTO.getLastInBlockNumber(), customStakingData.blockNumber));
        stakingDTO.addVin(customStakingData.amount);
      }
    }
  }

  /**
   * 
   */
  private Map<Integer, CustomStakingData> getDepositAddressToCustomStakingDataMap(int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("getDepositAddressToCustomStakingDataMap()");

    try {
      Map<Integer, CustomStakingData> depositAddressToStakingDataMap = new HashMap<>();

      customStakingVinSelectStatement.setInt(1, liquidityAddressNumber);

      ResultSet resultSet = customStakingVinSelectStatement.executeQuery();

      while (resultSet.next()) {
        CustomStakingData customStakingData = new CustomStakingData();

        customStakingData.tokenNumber = resultSet.getInt("token_number");
        customStakingData.blockNumber = resultSet.getInt("block_number");
        customStakingData.addressNumber = resultSet.getInt("address_number");
        customStakingData.amount = resultSet.getBigDecimal("sum_amount");

        depositAddressToStakingDataMap.put(customStakingData.addressNumber, customStakingData);
      }

      resultSet.close();

      return depositAddressToStakingDataMap;
    } catch (Exception e) {
      throw new DfxException("getDepositAddressToCustomStakingDataMap", e);
    }
  }

  /**
   * 
   */
  private void insertStaking(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("insertStaking()");

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
      throw new DfxException("insertStaking", e);
    }
  }

  /**
   * 
   */
  private void updateStaking(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("updateStaking()");

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
      throw new DfxException("updateStaking", e);
    }
  }

  /**
   * 
   */
  private class CustomStakingData {
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
