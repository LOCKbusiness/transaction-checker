package ch.dfx.tools.compare;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.math.BigDecimal;
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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.tools.compare.data.HistoryDTO;
import ch.dfx.tools.compare.data.HistoryDTOList;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class APIHistory {
  private static final Logger LOGGER = LogManager.getLogger(APIHistory.class);

  // ...
  private static final int HTTP_CLIENT_TIMEOUT = 10 * 1000;

  // ...
  private PreparedStatement stakingDepositSelectStatement = null;
  private PreparedStatement stakingCustomerSelectStatement = null;

  // ...
  private final NetworkEnum network;
  private final DatabaseBlockHelper databaseBlockHelper;
  private final Gson gson;

  /**
   * 
   */
  public APIHistory(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper) {
    this.network = network;
    this.databaseBlockHelper = databaseBlockHelper;

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
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
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public void checkDeposit(
      @Nonnull Connection connection,
      @Nonnull String liquidityAddress,
      @Nonnull String depositAddress) {
    try {
      openStatements(connection);

      BigDecimal apiTotalAmount = BigDecimal.ZERO;
      BigDecimal dbTotalAmount = BigDecimal.ZERO;

      HistoryDTOList depositHistoryDTOList = getDepositHistoryDTOList(depositAddress);
      List<TransactionData> depositTransactionDataList = getStakingDepositTransactionDataList(liquidityAddress, depositAddress);

      // ...
      Map<String, HistoryDTO> txIdToDepositHistoryDTOMap = new HashMap<>();
      depositHistoryDTOList.stream()
          .filter(dto -> "Deposit".equals(dto.getType()) || "Reward".equals(dto.getType()))
          .filter(dto -> "Confirmed".equals(dto.getStatus()))
          .filter(dto -> null != dto.getTxId())
          .forEach(dto -> txIdToDepositHistoryDTOMap.put(dto.getTxId(), dto));

      Map<String, TransactionData> txIdToTransactionDataMap = new HashMap<>();
      depositTransactionDataList.forEach(data -> txIdToTransactionDataMap.put(data.getInTxId(), data));

      // ...
      Set<String> txIdToDepositHistorySet = new HashSet<>(txIdToDepositHistoryDTOMap.keySet());
      txIdToDepositHistorySet.removeAll(txIdToTransactionDataMap.keySet());

      Set<String> txIdToTransactionSet = new HashSet<>(txIdToTransactionDataMap.keySet());
      txIdToTransactionSet.removeAll(txIdToDepositHistoryDTOMap.keySet());

      if (!txIdToDepositHistorySet.isEmpty()) {
        LOGGER.error("txIdToDepositHistorySet is not empty: " + txIdToDepositHistorySet);
      }

      if (!txIdToTransactionSet.isEmpty()) {
        LOGGER.error("txIdToTransactionSet is not empty: " + txIdToTransactionSet);
      }

      // ...
      for (String inTxId : txIdToTransactionDataMap.keySet()) {
        TransactionData depositTransactionData = txIdToTransactionDataMap.get(inTxId);

        HistoryDTO depositHistoryDTO = txIdToDepositHistoryDTOMap.get(inTxId);

        if (null == depositHistoryDTO) {
          throw new DfxException("TxId '" + inTxId + "' not in API History");
        }

        BigDecimal apiAmount = depositHistoryDTO.getInputAmount();
        BigDecimal dbamount = depositTransactionData.getAmount();

        if (0 != apiAmount.compareTo(dbamount)) {
          throw new DfxException("TxId '" + inTxId + "' different amount");
        }

        apiTotalAmount = apiTotalAmount.add(apiAmount);
        dbTotalAmount = dbTotalAmount.add(dbamount);
      }

      LOGGER.debug("Liquidity Address: " + liquidityAddress);
      LOGGER.debug("Deposit Address  : " + depositAddress);
      LOGGER.debug("Amount (API / DB): " + apiTotalAmount + " / " + dbTotalAmount);

      // ...
      closeStatements();
    } catch (Exception e) {
      LOGGER.error("getData", e);
    }
  }

  /**
   * 
   */
  public void checkWithdrawal(
      @Nonnull Connection connection,
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress) {
    try {
      openStatements(connection);

      BigDecimal apiTotalAmount = BigDecimal.ZERO;
      BigDecimal dbTotalAmount = BigDecimal.ZERO;

      HistoryDTOList customerHistoryDTOList = getCustomerHistoryDTOList(customerAddress);
      List<TransactionData> customerTransactionDataList = getStakingCustomerTransactionDataList(liquidityAddress, customerAddress);

      // ...
      Map<String, HistoryDTO> txIdToDepositHistoryDTOMap = new HashMap<>();
      customerHistoryDTOList.stream()
          .filter(dto -> "Withdrawal".equals(dto.getType()))
          .filter(dto -> "Confirmed".equals(dto.getStatus()))
          .filter(dto -> null != dto.getTxId())
          .forEach(dto -> txIdToDepositHistoryDTOMap.put(dto.getTxId(), dto));

      Map<String, TransactionData> txIdToTransactionDataMap = new HashMap<>();
      customerTransactionDataList.forEach(data -> txIdToTransactionDataMap.put(data.getOutTxId(), data));

      // ...
      Set<String> txIdToDepositHistorySet = new HashSet<>(txIdToDepositHistoryDTOMap.keySet());
      txIdToDepositHistorySet.removeAll(txIdToTransactionDataMap.keySet());

      Set<String> txIdToTransactionSet = new HashSet<>(txIdToTransactionDataMap.keySet());
      txIdToTransactionSet.removeAll(txIdToDepositHistoryDTOMap.keySet());

      if (!txIdToDepositHistorySet.isEmpty()) {
        LOGGER.error("txIdToDepositHistorySet is not empty: " + txIdToDepositHistorySet);
      }

      if (!txIdToTransactionSet.isEmpty()) {
        LOGGER.error("txIdToTransactionSet is not empty: " + txIdToTransactionSet);
      }

      // ...
      for (TransactionData customerTransactionData : customerTransactionDataList) {
        String outTxId = customerTransactionData.getOutTxId();

        HistoryDTO depositHistoryDTO = txIdToDepositHistoryDTOMap.get(outTxId);

        if (null == depositHistoryDTO) {
          throw new DfxException("TxId '" + outTxId + "' not in API History");
        }

        BigDecimal apiAmount = depositHistoryDTO.getOutputAmount();
        BigDecimal dbamount = customerTransactionData.getAmount();

        if (0 != apiAmount.compareTo(dbamount)) {
          throw new DfxException("TxId '" + outTxId + "' different amount");
        }

        apiTotalAmount = apiTotalAmount.add(apiAmount);
        dbTotalAmount = dbTotalAmount.add(dbamount);
      }

      LOGGER.debug("Liquidity Address: " + liquidityAddress);
      LOGGER.debug("Deposit Address  : " + customerAddress);
      LOGGER.debug("Amount (API / DB): " + apiTotalAmount + " / " + dbTotalAmount);

      // ...
      closeStatements();
    } catch (Exception e) {
      LOGGER.error("getData", e);
    }
  }

  /**
   * 
   */
  private HistoryDTOList getDepositHistoryDTOList(@Nonnull String depositAddress) throws DfxException {
    String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/analytics/history/Compact";
    url = url + "?depositAddress=" + depositAddress;
    url = url + "&type=json";
    LOGGER.debug("URL: " + url);

    return getHistoryDTOList(url);
  }

  /**
   * 
   */
  private HistoryDTOList getCustomerHistoryDTOList(@Nonnull String customerAddress) throws DfxException {
    String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/analytics/history/Compact";
    url = url + "?userAddress=" + customerAddress;
    url = url + "&type=json";
    LOGGER.debug("URL: " + url);

    return getHistoryDTOList(url);
  }

  /**
   * 
   */
  private HistoryDTOList getHistoryDTOList(@Nonnull String url) throws DfxException {
    try {
      LOGGER.debug("URL: " + url);

      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout(HTTP_CLIENT_TIMEOUT)
              .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT)
              .setSocketTimeout(HTTP_CLIENT_TIMEOUT).build();

      HttpClient httpClient =
          HttpClientBuilder.create()
              .setDefaultRequestConfig(requestConfig)
              .build();

      HttpGet httpGet = new HttpGet(url);
      HttpResponse httpResponse = httpClient.execute(httpGet);

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());

      HistoryDTOList historyDTOList = gson.fromJson(jsonResponse, HistoryDTOList.class);

      // ...
      historyDTOList.sort((d1, d2) -> d2.getDate().compareTo(d1.getDate()));

      return historyDTOList;
    } catch (Exception e) {
      throw new DfxException("getHistoryDTOList", e);
    }
  }

  /**
   * 
   */
  private List<TransactionData> getStakingDepositTransactionDataList(
      @Nonnull String liquidityAddress,
      @Nonnull String depositAddress) throws DfxException {
    LOGGER.trace("getStakingDepositTransactionDataList()");

    try {
      List<TransactionData> transactionDataList = new ArrayList<>();

      // ...
      AddressDTO depositAddressDTO = databaseBlockHelper.getAddressDTOByAddress(depositAddress);
      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress(liquidityAddress);

      if (null != depositAddressDTO
          && null != liquidityAddressDTO) {
        stakingDepositSelectStatement.setInt(1, depositAddressDTO.getNumber());
        stakingDepositSelectStatement.setInt(2, liquidityAddressDTO.getNumber());

        ResultSet resultSet = stakingDepositSelectStatement.executeQuery();

        while (resultSet.next()) {
          TransactionData transactionData = new TransactionData();
          transactionData.setDeposit(true);
          transactionData.setInTimestamp(new Timestamp(resultSet.getLong("in_timestamp") * 1000));
          transactionData.setInTxId(resultSet.getString("in_txid"));
          transactionData.setOutTimestamp(new Timestamp(resultSet.getLong("out_timestamp") * 1000));
          transactionData.setOutTxId(resultSet.getString("out_txid"));
          transactionData.setAmount(resultSet.getBigDecimal("vin"));

          transactionDataList.add(transactionData);
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
  private List<TransactionData> getStakingCustomerTransactionDataList(
      @Nonnull String liquidityAddress,
      @Nonnull String customerAddress) throws DfxException {
    LOGGER.trace("getStakingCustomerTransactionDataList()");

    try {
      List<TransactionData> transactionDataList = new ArrayList<>();

      // ...
      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress(liquidityAddress);
      AddressDTO customerAddressDTO = databaseBlockHelper.getAddressDTOByAddress(customerAddress);

      if (null != liquidityAddressDTO
          && null != customerAddressDTO) {
        stakingCustomerSelectStatement.setInt(1, liquidityAddressDTO.getNumber());
        stakingCustomerSelectStatement.setInt(2, customerAddressDTO.getNumber());

        ResultSet resultSet = stakingCustomerSelectStatement.executeQuery();

        while (resultSet.next()) {
          TransactionData transactionData = new TransactionData();
          transactionData.setDeposit(true);
          transactionData.setInTimestamp(new Timestamp(resultSet.getLong("in_timestamp") * 1000));
          transactionData.setInTxId(resultSet.getString("in_txid"));
          transactionData.setOutTimestamp(new Timestamp(resultSet.getLong("out_timestamp") * 1000));
          transactionData.setOutTxId(resultSet.getString("out_txid"));
          transactionData.setAmount(resultSet.getBigDecimal("vout"));

          transactionDataList.add(transactionData);
        }

        resultSet.close();
      }

      // ...
      transactionDataList.sort((d1, d2) -> d2.getInTimestamp().compareTo(d1.getInTimestamp()));

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getStakingCustomerTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private class TransactionData {
    private boolean isDeposit = false;
    private Timestamp inTimestamp = null;
    private String inTxId = null;
    private Timestamp outTimestamp = null;
    private String outTxId = null;
    private BigDecimal amount = null;

    private boolean isDeposit() {
      return isDeposit;
    }

    private void setDeposit(boolean isDeposit) {
      this.isDeposit = isDeposit;
    }

    public Timestamp getInTimestamp() {
      return inTimestamp;
    }

    public void setInTimestamp(Timestamp inTimestamp) {
      this.inTimestamp = inTimestamp;
    }

    public String getInTxId() {
      return inTxId;
    }

    public void setInTxId(String inTxId) {
      this.inTxId = inTxId;
    }

    public Timestamp getOutTimestamp() {
      return outTimestamp;
    }

    public void setOutTimestamp(Timestamp outTimestamp) {
      this.outTimestamp = outTimestamp;
    }

    public String getOutTxId() {
      return outTxId;
    }

    public void setOutTxId(String outTxId) {
      this.outTxId = outTxId;
    }

    private BigDecimal getAmount() {
      return amount;
    }

    private void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    @Override
    public String toString() {
      return TransactionCheckerUtils.toJson(this);
    }
  }
}
