package ch.dfx.reporting.compare;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.reporting.compare.data.APITransactionHistoryDTO;
import ch.dfx.reporting.compare.data.APITransactionHistoryDTOList;
import ch.dfx.reporting.compare.data.CompareInfoDTO;
import ch.dfx.reporting.compare.data.DBTransactionDTO;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class APITransactionHistoryProvider {
  private static final Logger LOGGER = LogManager.getLogger(APITransactionHistoryProvider.class);

  // ...
  private static final int SCALE = 8;

  // ...
  private PreparedStatement stakingDepositSelectStatement = null;
  private PreparedStatement stakingCustomerSelectStatement = null;

  private PreparedStatement yieldmachineDepositSelectStatement = null;
  private PreparedStatement yieldmachineCustomerSelectStatement = null;

  private PreparedStatement yieldmachineTransactionSelectStatement = null;

  // ...
  private final NetworkEnum network;
  private final DatabaseBlockHelper databaseBlockHelper;

  /**
   * 
   */
  public APITransactionHistoryProvider(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper) {
    this.network = network;
    this.databaseBlockHelper = databaseBlockHelper;
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Staking ...
      String stakingDepositSelectSql =
          "SELECT"
              + " b_in.timestamp AS in_timestamp,"
              + " t_in.txid AS in_txid,"
              + " b_out.timestamp AS out_timestamp,"
              + " t_out.txid AS out_txid,"
              + " at_in.vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b_out"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t_out ON"
              + " b_out.number = t_out.block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " t_out.block_number = at_out.block_number"
              + " AND t_out.number = at_out.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".block b_in ON"
              + " b_in.number = at_in.in_block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t_in ON"
              + " t_in.block_number = at_in.in_block_number"
              + " AND t_in.number = at_in.in_transaction_number"
              + " WHERE"
              + " at_in.address_number=?"
              + " AND at_out.address_number=?"
              + " GROUP BY"
              + " b_in.timestamp,"
              + " b_out.timestamp,"
              + " t_in.txid,"
              + " t_out.txid,"
              + " at_in.vin";
      stakingDepositSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingDepositSelectSql));

      String stakingCustomerSelectSql =
          "SELECT"
              + " b_in.timestamp AS in_timestamp,"
              + " t_in.txid AS in_txid,"
              + " b_out.timestamp AS out_timestamp,"
              + " t_out.txid AS out_txid,"
              + " at_out.vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b_out"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t_out ON"
              + " b_out.number = t_out.block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " t_out.block_number = at_out.block_number"
              + " AND t_out.number = at_out.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".block b_in ON"
              + " b_in.number = at_in.in_block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t_in ON"
              + " t_in.block_number = at_in.in_block_number"
              + " AND t_in.number = at_in.in_transaction_number"
              + " WHERE"
              + " at_in.address_number=?"
              + " AND at_out.address_number=?"
              + " GROUP BY"
              + " b_in.timestamp,"
              + " b_out.timestamp,"
              + " t_in.txid,"
              + " t_out.txid,"
              + " at_out.vout";
      stakingCustomerSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingCustomerSelectSql));

      // Yield Machine ...
      String yieldmachineDepositSelectSql =
          "SELECT"
              + " b.timestamp,"
              + " t.txid,"
              + " ata_in.amount"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t ON"
              + " b.number = t.block_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out ON"
              + " t.block_number = ata_out.block_number"
              + " AND t.number = ata_out.transaction_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number = ata_in.type_number"
              + " AND ata_out.address_number != ata_in.address_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " ata_in.token_number=?"
              + " AND ata_in.address_number=?"
              + " AND ata_out.address_number=?"
              + " GROUP BY"
              + " b.timestamp,"
              + " t.txid,"
              + " ata_in.amount";
      yieldmachineDepositSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, yieldmachineDepositSelectSql));

      String yieldmachineCustomerSelectSql =
          "SELECT"
              + " b.timestamp,"
              + " t.txid,"
              + " ata_out.amount"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t ON"
              + " b.number = t.block_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out ON"
              + " t.block_number = ata_out.block_number"
              + " AND t.number = ata_out.transaction_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number = ata_in.type_number"
              + " AND ata_out.address_number != ata_in.address_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " ata_out.token_number=?"
              + " AND ata_in.address_number=?"
              + " AND ata_out.address_number=?"
              + " GROUP BY"
              + " b.timestamp,"
              + " t.txid,"
              + " ata_out.amount";

      yieldmachineCustomerSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, yieldmachineCustomerSelectSql));

      String yieldmachineTransactionSelectSql =
          "SELECT"
              + " b.timestamp,"
              + " t.txid"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t ON"
              + "  b.number = t.block_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out ON"
              + " t.block_number = ata_out.block_number"
              + " AND t.number = ata_out.transaction_number"
              + " WHERE"
              + " ata_out.address_number=?"
              + " AND ata_out.amount=?";
      yieldmachineTransactionSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, yieldmachineTransactionSelectSql));
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
      stakingCustomerSelectStatement.close();

      yieldmachineDepositSelectStatement.close();
      yieldmachineCustomerSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public CompareInfoDTO checkStakingDeposit(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String depositAddress,
      @Nonnull APISelector apiSelector) {
    LOGGER.debug("checkStakingDeposit()");

    CompareInfoDTO compareInfoDTO = new CompareInfoDTO();

    try {
      compareInfoDTO.setLiquidityAddress(liquidityAddress);
      compareInfoDTO.setDepositAddress(depositAddress);

      openStatements(connection);

      BigDecimal apiTotalAmount = BigDecimal.ZERO;
      BigDecimal dbTotalAmount = BigDecimal.ZERO;

      APITransactionHistoryDTOList apiTransactionHistoryDTOList = apiSelector.getDepositTransactionHistoryDTOList(depositAddress);
      List<DBTransactionDTO> dbTransactionDTOList = getStakingDepositTransactionDataList(token, liquidityAddress, depositAddress);

      // ...
      Map<String, APITransactionHistoryDTO> txIdToAPITransactionHistoryDTOMap = new HashMap<>();
      apiTransactionHistoryDTOList.stream()
          .filter(dto -> "Deposit".equals(dto.getType()) || "Reward".equals(dto.getType()))
          .filter(dto -> "Confirmed".equals(dto.getStatus()))
          .filter(dto -> null != dto.getTxId())
          .forEach(dto -> txIdToAPITransactionHistoryDTOMap.put(dto.getTxId(), dto));

      Map<String, DBTransactionDTO> txIdToDBTransactionDTOMap = new HashMap<>();
      dbTransactionDTOList.forEach(data -> txIdToDBTransactionDTOMap.put(data.getInTxId(), data));

      // ...
      Set<String> txIdToAPITransactionHistoryDTOSet = new HashSet<>(txIdToAPITransactionHistoryDTOMap.keySet());
      txIdToAPITransactionHistoryDTOSet.removeAll(txIdToDBTransactionDTOMap.keySet());

      Set<String> txIdToDBTransactionDTOSet = new HashSet<>(txIdToDBTransactionDTOMap.keySet());
      txIdToDBTransactionDTOSet.removeAll(txIdToAPITransactionHistoryDTOMap.keySet());

      if (!txIdToAPITransactionHistoryDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToAPITransactionHistoryDTOSet(txIdToAPITransactionHistoryDTOSet);
      }

      if (!txIdToDBTransactionDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToDBTransactionDTOSet(txIdToDBTransactionDTOSet);
      }

      // ...
      for (String inTxId : txIdToDBTransactionDTOMap.keySet()) {
        DBTransactionDTO dbTransactionDTO = txIdToDBTransactionDTOMap.get(inTxId);

        APITransactionHistoryDTO apiTransactionHistoryDTO = txIdToAPITransactionHistoryDTOMap.get(inTxId);

        if (null == apiTransactionHistoryDTO) {
          compareInfoDTO.addApiTransactionHistoryDTO(APITransactionHistoryDTO.createUnknownAPITransactionHistoryDTO());
          compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
        } else {
          BigDecimal apiAmount = apiTransactionHistoryDTO.getInputAmount().setScale(SCALE, RoundingMode.HALF_UP);
          BigDecimal dbAmount = dbTransactionDTO.getAmount().setScale(SCALE, RoundingMode.HALF_UP);

          if (0 != apiAmount.compareTo(dbAmount)) {
            compareInfoDTO.addApiTransactionHistoryDTO(apiTransactionHistoryDTO);
            compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
          }

          apiTotalAmount = apiTotalAmount.add(apiAmount);
          dbTotalAmount = dbTotalAmount.add(dbAmount);
        }
      }

      compareInfoDTO.setApiTotalAmount(apiTotalAmount);
      compareInfoDTO.setDbTotalAmount(dbTotalAmount);

      // ...
      closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkStakingDeposit", e);
    }

    return compareInfoDTO;
  }

  /**
   * 
   */
  public CompareInfoDTO checkStakingWithdrawal(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress,
      @Nonnull APISelector apiSelector) {
    LOGGER.debug("checkStakingWithdrawal()");

    CompareInfoDTO compareInfoDTO = new CompareInfoDTO();

    try {
      compareInfoDTO.setLiquidityAddress(liquidityAddress);
      compareInfoDTO.setCustomerAddress(customerAddress);

      openStatements(connection);

      BigDecimal apiTotalAmount = BigDecimal.ZERO;
      BigDecimal dbTotalAmount = BigDecimal.ZERO;

      APITransactionHistoryDTOList apiTransactionHistoryDTOList = apiSelector.getCustomerTransactionHistoryDTOList(customerAddress);
      List<DBTransactionDTO> dbTransactionDTOList = getStakingWithdrawalTransactionDataList(token, liquidityAddress, customerAddress);

      // ...
      Map<String, APITransactionHistoryDTO> txIdToAPITransactionHistoryDTOMap = new HashMap<>();
      apiTransactionHistoryDTOList.stream()
          .filter(dto -> "Withdrawal".equals(dto.getType()))
          .filter(dto -> "Confirmed".equals(dto.getStatus()))
          .filter(dto -> "Masternode".equals(dto.getSource()))
          .filter(dto -> null != dto.getTxId())
          .forEach(dto -> txIdToAPITransactionHistoryDTOMap.put(dto.getTxId(), dto));

      Map<String, DBTransactionDTO> txIdToDBTransactionDTOMap = new HashMap<>();
      dbTransactionDTOList.forEach(data -> txIdToDBTransactionDTOMap.put(data.getOutTxId(), data));

      // ...
      Set<String> txIdToAPITransactionHistoryDTOSet = new HashSet<>(txIdToAPITransactionHistoryDTOMap.keySet());
      txIdToAPITransactionHistoryDTOSet.removeAll(txIdToDBTransactionDTOMap.keySet());

      Set<String> txIdToDBTransactionDTOSet = new HashSet<>(txIdToDBTransactionDTOMap.keySet());
      txIdToDBTransactionDTOSet.removeAll(txIdToAPITransactionHistoryDTOMap.keySet());

      if (!txIdToAPITransactionHistoryDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToAPITransactionHistoryDTOSet(txIdToAPITransactionHistoryDTOSet);
      }

      if (!txIdToDBTransactionDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToDBTransactionDTOSet(txIdToDBTransactionDTOSet);
      }

      // ...
      for (String outTxId : txIdToDBTransactionDTOMap.keySet()) {
        DBTransactionDTO dbTransactionDTO = txIdToDBTransactionDTOMap.get(outTxId);

        APITransactionHistoryDTO apiTransactionHistoryDTO = txIdToAPITransactionHistoryDTOMap.get(outTxId);

        if (null == apiTransactionHistoryDTO) {
          compareInfoDTO.addApiTransactionHistoryDTO(APITransactionHistoryDTO.createUnknownAPITransactionHistoryDTO());
          compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
        } else {
          BigDecimal apiAmount = apiTransactionHistoryDTO.getOutputAmount().setScale(SCALE, RoundingMode.HALF_UP);
          BigDecimal dbAmount = dbTransactionDTO.getAmount().setScale(SCALE, RoundingMode.HALF_UP);

          if (0 != apiAmount.compareTo(dbAmount)) {
            compareInfoDTO.addApiTransactionHistoryDTO(apiTransactionHistoryDTO);
            compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
          }

          apiTotalAmount = apiTotalAmount.add(apiAmount);
          dbTotalAmount = dbTotalAmount.add(dbAmount);
        }
      }

      compareInfoDTO.setApiTotalAmount(apiTotalAmount);
      compareInfoDTO.setDbTotalAmount(dbTotalAmount);

      // ...
      closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkStakingWithdrawal", e);
    }

    return compareInfoDTO;
  }

  /**
   * 
   */
  private List<DBTransactionDTO> getStakingDepositTransactionDataList(
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String depositAddress) throws DfxException {
    LOGGER.trace("getStakingDepositTransactionDataList()");

    try {
      List<DBTransactionDTO> transactionDataList = new ArrayList<>();

      // ...
      AddressDTO depositAddressDTO = databaseBlockHelper.getAddressDTOByAddress(depositAddress);
      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress(liquidityAddress);

      if (null != depositAddressDTO
          && null != liquidityAddressDTO) {
        stakingDepositSelectStatement.setInt(1, depositAddressDTO.getNumber());
        stakingDepositSelectStatement.setInt(2, liquidityAddressDTO.getNumber());

        ResultSet resultSet = stakingDepositSelectStatement.executeQuery();

        while (resultSet.next()) {
          DBTransactionDTO dbTransactionDTO = new DBTransactionDTO(token);

          dbTransactionDTO.setInTimestamp(new Timestamp(resultSet.getLong("in_timestamp") * 1000));
          dbTransactionDTO.setInTxId(resultSet.getString("in_txid"));
          dbTransactionDTO.setOutTimestamp(new Timestamp(resultSet.getLong("out_timestamp") * 1000));
          dbTransactionDTO.setOutTxId(resultSet.getString("out_txid"));
          dbTransactionDTO.setAmount(resultSet.getBigDecimal("vin"));

          transactionDataList.add(dbTransactionDTO);
        }

        resultSet.close();
      }

      // ...
      transactionDataList.sort((d1, d2) -> d2.getInTimestamp().compareTo(d1.getInTimestamp()));

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getStakingDepositTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private List<DBTransactionDTO> getStakingWithdrawalTransactionDataList(
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress) throws DfxException {
    LOGGER.trace("getStakingWithdrawalTransactionDataList()");

    try {
      List<DBTransactionDTO> transactionDataList = new ArrayList<>();

      // ...
      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress(liquidityAddress);
      AddressDTO customerAddressDTO = databaseBlockHelper.getAddressDTOByAddress(customerAddress);

      if (null != liquidityAddressDTO
          && null != customerAddressDTO) {
        stakingCustomerSelectStatement.setInt(1, liquidityAddressDTO.getNumber());
        stakingCustomerSelectStatement.setInt(2, customerAddressDTO.getNumber());

        ResultSet resultSet = stakingCustomerSelectStatement.executeQuery();

        while (resultSet.next()) {
          DBTransactionDTO dbTransactionDTO = new DBTransactionDTO(token);

          dbTransactionDTO.setInTimestamp(new Timestamp(resultSet.getLong("in_timestamp") * 1000));
          dbTransactionDTO.setInTxId(resultSet.getString("in_txid"));
          dbTransactionDTO.setOutTimestamp(new Timestamp(resultSet.getLong("out_timestamp") * 1000));
          dbTransactionDTO.setOutTxId(resultSet.getString("out_txid"));
          dbTransactionDTO.setAmount(resultSet.getBigDecimal("vout"));

          transactionDataList.add(dbTransactionDTO);
        }

        resultSet.close();
      }

      // ...
      transactionDataList.sort((d1, d2) -> d2.getInTimestamp().compareTo(d1.getInTimestamp()));

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getStakingWithdrawalTransactionDataList", e);
    }
  }

  /**
   * 
   */
  public CompareInfoDTO checkYieldmachineDeposit(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String depositAddress,
      @Nonnull APISelector apiSelector) {
    LOGGER.debug("checkYieldmachineDeposit()");

    CompareInfoDTO compareInfoDTO = new CompareInfoDTO();

    try {
      compareInfoDTO.setLiquidityAddress(liquidityAddress);
      compareInfoDTO.setDepositAddress(depositAddress);

      openStatements(connection);

      BigDecimal apiTotalAmount = BigDecimal.ZERO;
      BigDecimal dbTotalAmount = BigDecimal.ZERO;

      APITransactionHistoryDTOList apiTransactionHistoryDTOList = apiSelector.getDepositTransactionHistoryDTOList(depositAddress);
      List<DBTransactionDTO> dbTransactionDTOList = getYieldmachineDepositTransactionDataList(token, liquidityAddress, depositAddress);

      // ...
      Map<String, APITransactionHistoryDTO> txIdToAPITransactionHistoryDTOMap = new HashMap<>();
      apiTransactionHistoryDTOList.stream()
          .filter(dto -> "Deposit".equals(dto.getType()) || "Reward".equals(dto.getType()))
          .filter(dto -> "Confirmed".equals(dto.getStatus()))
          .filter(dto -> token.toString().equals(dto.getInputAsset()))
          .filter(dto -> null != dto.getTxId())
          .forEach(dto -> txIdToAPITransactionHistoryDTOMap.put(dto.getTxId(), dto));

      Map<String, DBTransactionDTO> txIdToDBTransactionDTOMap = new HashMap<>();
      dbTransactionDTOList.forEach(data -> txIdToDBTransactionDTOMap.put(data.getInTxId(), data));

      // ...
      Set<String> txIdToAPITransactionHistoryDTOSet = new HashSet<>(txIdToAPITransactionHistoryDTOMap.keySet());
      txIdToAPITransactionHistoryDTOSet.removeAll(txIdToDBTransactionDTOMap.keySet());

      Set<String> txIdToDBTransactionDTOSet = new HashSet<>(txIdToDBTransactionDTOMap.keySet());
      txIdToDBTransactionDTOSet.removeAll(txIdToAPITransactionHistoryDTOMap.keySet());

      if (!txIdToAPITransactionHistoryDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToAPITransactionHistoryDTOSet(txIdToAPITransactionHistoryDTOSet);
      }

      if (!txIdToDBTransactionDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToDBTransactionDTOSet(txIdToDBTransactionDTOSet);
      }

      // ...
      for (String inTxId : txIdToDBTransactionDTOMap.keySet()) {
        DBTransactionDTO dbTransactionDTO = txIdToDBTransactionDTOMap.get(inTxId);

        APITransactionHistoryDTO apiTransactionHistoryDTO = txIdToAPITransactionHistoryDTOMap.get(inTxId);

        if (null == apiTransactionHistoryDTO) {
          compareInfoDTO.addApiTransactionHistoryDTO(APITransactionHistoryDTO.createUnknownAPITransactionHistoryDTO());
          compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
        } else {
          BigDecimal apiAmount = apiTransactionHistoryDTO.getInputAmount().setScale(SCALE, RoundingMode.HALF_UP);
          BigDecimal dbAmount = dbTransactionDTO.getAmount().setScale(SCALE, RoundingMode.HALF_UP);

          if (0 != apiAmount.compareTo(dbAmount)) {
            compareInfoDTO.addApiTransactionHistoryDTO(apiTransactionHistoryDTO);
            compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
          }

          apiTotalAmount = apiTotalAmount.add(apiAmount);
          dbTotalAmount = dbTotalAmount.add(dbAmount);
        }
      }

      compareInfoDTO.setApiTotalAmount(apiTotalAmount);
      compareInfoDTO.setDbTotalAmount(dbTotalAmount);

      // ...
      closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkYieldmachineDeposit", e);
    }

    return compareInfoDTO;
  }

  /**
   * 
   */
  public CompareInfoDTO checkYieldmachineWithdrawal(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress,
      @Nonnull APISelector apiSelector) {
    LOGGER.debug("checkYieldmachineWithdrawal()");

    CompareInfoDTO compareInfoDTO = new CompareInfoDTO();

    try {
      compareInfoDTO.setLiquidityAddress(liquidityAddress);
      compareInfoDTO.setCustomerAddress(customerAddress);

      openStatements(connection);

      BigDecimal apiTotalAmount = BigDecimal.ZERO;
      BigDecimal dbTotalAmount = BigDecimal.ZERO;

      APITransactionHistoryDTOList apiTransactionHistoryDTOList = apiSelector.getCustomerTransactionHistoryDTOList(customerAddress);
      List<DBTransactionDTO> dbTransactionDTOList = getYieldmachineWithdrawalTransactionDataList(token, liquidityAddress, customerAddress);

      // ...
      Map<String, APITransactionHistoryDTO> txIdToAPITransactionHistoryDTOMap = new HashMap<>();
      apiTransactionHistoryDTOList.stream()
          .filter(dto -> "Withdrawal".equals(dto.getType()))
          .filter(dto -> "Confirmed".equals(dto.getStatus()))
          .filter(dto -> "LiquidityMining".equals(dto.getSource()))
          .filter(dto -> token.toString().equals(dto.getOutputAsset()))
          .filter(dto -> null != dto.getTxId())
          .forEach(dto -> txIdToAPITransactionHistoryDTOMap.put(dto.getTxId(), dto));

      Map<String, DBTransactionDTO> txIdToDBTransactionDTOMap = new HashMap<>();
      dbTransactionDTOList.forEach(data -> txIdToDBTransactionDTOMap.put(data.getOutTxId(), data));

      // ...
      Set<String> txIdToAPITransactionHistoryDTOSet = new HashSet<>(txIdToAPITransactionHistoryDTOMap.keySet());
      txIdToAPITransactionHistoryDTOSet.removeAll(txIdToDBTransactionDTOMap.keySet());

      Set<String> txIdToDBTransactionDTOSet = new HashSet<>(txIdToDBTransactionDTOMap.keySet());
      txIdToDBTransactionDTOSet.removeAll(txIdToAPITransactionHistoryDTOMap.keySet());

      if (!txIdToAPITransactionHistoryDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToAPITransactionHistoryDTOSet(txIdToAPITransactionHistoryDTOSet);
      }

      if (!txIdToDBTransactionDTOSet.isEmpty()) {
        compareInfoDTO.setTxIdToDBTransactionDTOSet(txIdToDBTransactionDTOSet);
      }

      // ...
      for (String outTxId : txIdToDBTransactionDTOMap.keySet()) {
        DBTransactionDTO dbTransactionDTO = txIdToDBTransactionDTOMap.get(outTxId);

        APITransactionHistoryDTO apiTransactionHistoryDTO = txIdToAPITransactionHistoryDTOMap.get(outTxId);

        if (null == apiTransactionHistoryDTO) {
          compareInfoDTO.addApiTransactionHistoryDTO(APITransactionHistoryDTO.createUnknownAPITransactionHistoryDTO());
          compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
        } else {
          BigDecimal apiAmount = apiTransactionHistoryDTO.getOutputAmount().setScale(SCALE, RoundingMode.HALF_UP);
          BigDecimal dbAmount = dbTransactionDTO.getAmount().setScale(SCALE, RoundingMode.HALF_UP);

          if (0 != apiAmount.compareTo(dbAmount)) {
            compareInfoDTO.addApiTransactionHistoryDTO(apiTransactionHistoryDTO);
            compareInfoDTO.addDBTransactionDTO(dbTransactionDTO);
          }

          apiTotalAmount = apiTotalAmount.add(apiAmount);
          dbTotalAmount = dbTotalAmount.add(dbAmount);
        }
      }

      compareInfoDTO.setApiTotalAmount(apiTotalAmount);
      compareInfoDTO.setDbTotalAmount(dbTotalAmount);

      // ...
      closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkYieldmachineWithdrawal", e);
    }

    return compareInfoDTO;
  }

  /**
   * 
   */
  private List<DBTransactionDTO> getYieldmachineDepositTransactionDataList(
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String depositAddress) throws DfxException {
    LOGGER.trace("getYieldmachineDepositTransactionDataList()");

    try {
      List<DBTransactionDTO> transactionDataList = new ArrayList<>();

      // ...
      AddressDTO depositAddressDTO = databaseBlockHelper.getAddressDTOByAddress(depositAddress);
      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress(liquidityAddress);

      if (null != depositAddressDTO
          && null != liquidityAddressDTO) {
        yieldmachineDepositSelectStatement.setInt(1, token.getNumber());
        yieldmachineDepositSelectStatement.setInt(2, depositAddressDTO.getNumber());
        yieldmachineDepositSelectStatement.setInt(3, liquidityAddressDTO.getNumber());

        ResultSet resultSet = yieldmachineDepositSelectStatement.executeQuery();

        while (resultSet.next()) {
          DBTransactionDTO dbTransactionDTO = new DBTransactionDTO(token);

          dbTransactionDTO.setOutTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
          dbTransactionDTO.setOutTxId(resultSet.getString("txid"));
          dbTransactionDTO.setAmount(resultSet.getBigDecimal("amount"));

          fillYieldmaschineDBTransactionDTO(depositAddress, dbTransactionDTO);

          transactionDataList.add(dbTransactionDTO);
        }

        resultSet.close();
      }

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getYieldmachineDepositTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private List<DBTransactionDTO> getYieldmachineWithdrawalTransactionDataList(
      @Nonnull TokenEnum token,
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress) throws DfxException {
    LOGGER.trace("getYieldmachineWithdrawalTransactionDataList()");

    try {
      List<DBTransactionDTO> transactionDataList = new ArrayList<>();

      // ...
      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress(liquidityAddress);
      AddressDTO customerAddressDTO = databaseBlockHelper.getAddressDTOByAddress(customerAddress);

      if (null != liquidityAddressDTO
          && null != customerAddressDTO) {
        yieldmachineCustomerSelectStatement.setInt(1, token.getNumber());
        yieldmachineCustomerSelectStatement.setInt(2, liquidityAddressDTO.getNumber());
        yieldmachineCustomerSelectStatement.setInt(3, customerAddressDTO.getNumber());

        ResultSet resultSet = yieldmachineCustomerSelectStatement.executeQuery();

        while (resultSet.next()) {
          DBTransactionDTO dbTransactionDTO = new DBTransactionDTO(token);

          dbTransactionDTO.setInTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
          dbTransactionDTO.setInTxId(resultSet.getString("txid"));
          dbTransactionDTO.setAmount(resultSet.getBigDecimal("amount"));

          dbTransactionDTO.setOutTimestamp(dbTransactionDTO.getInTimestamp());
          dbTransactionDTO.setOutTxId(dbTransactionDTO.getInTxId());

          transactionDataList.add(dbTransactionDTO);
        }

        resultSet.close();
      }

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getYieldmachineWithdrawalTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private void fillYieldmaschineDBTransactionDTO(
      @Nonnull String address,
      @Nonnull DBTransactionDTO dbTransactionDTO) throws DfxException {
    LOGGER.trace("getYieldmaschineTransaction()");

    try {
      AddressDTO addressDTO = databaseBlockHelper.getAddressDTOByAddress(address);

      if (null != addressDTO) {
        yieldmachineTransactionSelectStatement.setInt(1, addressDTO.getNumber());
        yieldmachineTransactionSelectStatement.setBigDecimal(2, dbTransactionDTO.getAmount());

        ResultSet resultSet = yieldmachineTransactionSelectStatement.executeQuery();

        if (resultSet.next()) {
          dbTransactionDTO.setInTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
          dbTransactionDTO.setInTxId(resultSet.getString("txid"));
        }

      }
    } catch (Exception e) {
      throw new DfxException("getYieldmaschineTransaction", e);
    }
  }
}
