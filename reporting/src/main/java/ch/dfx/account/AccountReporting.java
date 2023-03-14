package ch.dfx.account;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.reporting.Reporting;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class AccountReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(AccountReporting.class);

  // ...
  private PreparedStatement stakingDepositSelectStatement = null;
  private PreparedStatement stakingCustomerSelectStatement = null;

  private PreparedStatement yieldmachineDepositSelectStatement = null;
  private PreparedStatement yieldmachineCustomerSelectStatement = null;

  /**
   * 
   */
  public AccountReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);
  }

  /**
   * 
   */
  public void reportStaking(
      @Nonnull Connection connection,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress("df1qqkzs9mrlwq0qy0dw74wpvyuup0gwhak7p3m4cg");
      AddressDTO depositAddressDTO = databaseBlockHelper.getAddressDTOByAddress("xxxxx");
      AddressDTO customerAddressDTO = databaseBlockHelper.getAddressDTOByAddress("yyyyy");

      if (null != liquidityAddressDTO
          && null != depositAddressDTO
          && null != customerAddressDTO) {
        int liquidityAddressNumber = liquidityAddressDTO.getNumber();
        int depositAddressNumber = depositAddressDTO.getNumber();
        int customerAddressNumber = customerAddressDTO.getNumber();

        List<TransactionData> depositTransactionDataList =
            getStakingDepositTransactionDataList(liquidityAddressNumber, depositAddressNumber);

        List<TransactionData> customerTransactionDataList =
            getStakingCustomerTransactionDataList(liquidityAddressNumber, customerAddressNumber);

        List<TransactionData> transactionDataList = new ArrayList<>();
        transactionDataList.addAll(depositTransactionDataList);
        transactionDataList.addAll(customerTransactionDataList);

        // ...
        fillTotalBalance(transactionDataList);

        // ...
        Comparator<TransactionData> comparator =
            Comparator.comparing(TransactionData::getTimestamp).reversed();
        transactionDataList.sort(comparator);

        doReport(transactionDataList, rootPath, fileName, sheet);
      }

      // ...
      closeStatements();
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  public void reportYieldmachine(
      @Nonnull Connection connection,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      AddressDTO liquidityAddressDTO = databaseBlockHelper.getAddressDTOByAddress("df1qasdh79grpyd2wltpamznqp2xdhq6kmkevafxgq");
      AddressDTO depositAddressDTO = databaseBlockHelper.getAddressDTOByAddress("zzzzz");
      AddressDTO customerAddressDTO = databaseBlockHelper.getAddressDTOByAddress("yyyyy");

      if (null != liquidityAddressDTO
          && null != depositAddressDTO
          && null != customerAddressDTO) {
        int liquidityAddressNumber = liquidityAddressDTO.getNumber();
        int depositAddressNumber = depositAddressDTO.getNumber();
        int customerAddressNumber = customerAddressDTO.getNumber();

        List<TransactionData> depositTransactionDataList =
            getYieldmachineDepositTransactionDataList(token, liquidityAddressNumber, depositAddressNumber);

        List<TransactionData> customerTransactionDataList =
            getYieldmachineCustomerTransactionDataList(token, liquidityAddressNumber, customerAddressNumber);

        List<TransactionData> transactionDataList = new ArrayList<>();
        transactionDataList.addAll(depositTransactionDataList);
        transactionDataList.addAll(customerTransactionDataList);

        // ...
        fillTotalBalance(transactionDataList);

        // ...
        Comparator<TransactionData> comparator =
            Comparator.comparing(TransactionData::getTimestamp).reversed();
        transactionDataList.sort(comparator);

        doReport(transactionDataList, rootPath, fileName, sheet);
      }

      // ...
      closeStatements();
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void fillTotalBalance(@Nonnull List<TransactionData> transactionDataList) {
    Comparator<TransactionData> comparator =
        Comparator.comparing(TransactionData::getTimestamp);
    transactionDataList.sort(comparator);

    BigDecimal totalBalance = BigDecimal.ZERO;

    for (TransactionData transactionData : transactionDataList) {
      BigDecimal amount = transactionData.getAmount();

      if (transactionData.isDeposit()) {
        totalBalance = totalBalance.add(amount);
      } else {
        totalBalance = totalBalance.subtract(amount);
      }

      transactionData.setTotalBalance(totalBalance);
    }
  }

  /**
   * 
   */
  private void doReport(
      @Nonnull List<TransactionData> transactionDataList,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet) throws DfxException {
    // ...
    RowDataList rowDataList = new RowDataList(1);
    CellDataList cellDataList = new CellDataList();

    for (TransactionData transactionData : transactionDataList) {
      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(transactionData.getTimestamp()));
      rowData.addCellData(new CellData().setValue(transactionData.getTxId()));

      if (transactionData.isDeposit()) {
        rowData.addCellData(new CellData().setValue(transactionData.getAmount()));
        rowData.addCellData(new CellData().setValue(""));
      } else {
        rowData.addCellData(new CellData().setValue(""));
        rowData.addCellData(new CellData().setValue(transactionData.getAmount()));
      }

      rowData.addCellData(new CellData().setValue(transactionData.getTotalBalance()));

      rowDataList.add(rowData);
    }

    // ...
    openExcel(rootPath, fileName, sheet);

    cleanExcel(1);

    writeExcel(rowDataList, cellDataList);
    closeExcel();
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
              + " b.timestamp,"
              + " t.txid,"
              + " at_in.vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t ON"
              + " b.number = t.block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " t.block_number = at_out.block_number"
              + " AND t.number = at_out.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " at_in.address_number=?"
              + " AND at_out.address_number=?"
              + " GROUP BY"
              + " b.timestamp,"
              + " t.txid,"
              + " at_in.vin";
      stakingDepositSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingDepositSelectSql));

      // ...
      String stakingCustomerSelectSql =
          "SELECT"
              + " b.timestamp,"
              + " t.txid,"
              + " at_out.vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t ON"
              + " b.number = t.block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " t.block_number = at_out.block_number"
              + " AND t.number = at_out.transaction_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " at_in.address_number=?"
              + " AND at_out.address_number=?"
              + " GROUP BY"
              + " b.timestamp,"
              + " t.txid,"
              + " at_out.vout";
      stakingCustomerSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingCustomerSelectSql));

      // ...
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
              + " ata_out.token_number=?"
              + " AND ata_out.block_number>=-1"
              + " AND ata_out.address_number=?"
              + " AND ata_in.address_number=?"
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
              + " AND ata_out.block_number>=-1"
              + " AND ata_out.address_number=?"
              + " AND ata_in.address_number=?"
              + " GROUP BY"
              + " b.timestamp,"
              + " t.txid,"
              + " ata_out.amount";
      yieldmachineCustomerSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, yieldmachineCustomerSelectSql));
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
  private List<TransactionData> getStakingDepositTransactionDataList(
      int liquidityAddressNumber,
      int depositAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDepositTransactionDataList()");

    try {
      List<TransactionData> transactionDataList = new ArrayList<>();

      stakingDepositSelectStatement.setInt(1, depositAddressNumber);
      stakingDepositSelectStatement.setInt(2, liquidityAddressNumber);

      ResultSet resultSet = stakingDepositSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionData transactionData = new TransactionData();
        transactionData.setDeposit(true);
        transactionData.setTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
        transactionData.setTxId(resultSet.getString("txid"));
        transactionData.setAmount(resultSet.getBigDecimal("vin"));

        transactionDataList.add(transactionData);
      }

      resultSet.close();

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getStakingDepositTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private List<TransactionData> getStakingCustomerTransactionDataList(
      int liquidityAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("getStakingCustomerTransactionDataList()");

    try {
      List<TransactionData> transactionDataList = new ArrayList<>();

      stakingCustomerSelectStatement.setInt(1, liquidityAddressNumber);
      stakingCustomerSelectStatement.setInt(2, customerAddressNumber);

      ResultSet resultSet = stakingCustomerSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionData transactionData = new TransactionData();
        transactionData.setDeposit(false);
        transactionData.setTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
        transactionData.setTxId(resultSet.getString("txid"));
        transactionData.setAmount(resultSet.getBigDecimal("vout"));

        transactionDataList.add(transactionData);
      }

      resultSet.close();

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getStakingCustomerTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private List<TransactionData> getYieldmachineDepositTransactionDataList(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      int depositAddressNumber) throws DfxException {
    LOGGER.trace("getYieldmachineDepositTransactionDataList()");

    try {
      List<TransactionData> transactionDataList = new ArrayList<>();

      yieldmachineDepositSelectStatement.setInt(1, token.getNumber());
      yieldmachineDepositSelectStatement.setInt(2, liquidityAddressNumber);
      yieldmachineDepositSelectStatement.setInt(3, depositAddressNumber);

      ResultSet resultSet = yieldmachineDepositSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionData transactionData = new TransactionData();
        transactionData.setDeposit(true);
        transactionData.setTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
        transactionData.setTxId(resultSet.getString("txid"));
        transactionData.setAmount(resultSet.getBigDecimal("amount"));

        transactionDataList.add(transactionData);
      }

      resultSet.close();

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getYieldmachineDepositTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private List<TransactionData> getYieldmachineCustomerTransactionDataList(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("getYieldmachineCustomerTransactionDataList()");

    try {
      List<TransactionData> transactionDataList = new ArrayList<>();

      yieldmachineCustomerSelectStatement.setInt(1, token.getNumber());
      yieldmachineCustomerSelectStatement.setInt(2, customerAddressNumber);
      yieldmachineCustomerSelectStatement.setInt(3, liquidityAddressNumber);

      ResultSet resultSet = yieldmachineCustomerSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionData transactionData = new TransactionData();
        transactionData.setDeposit(false);
        transactionData.setTimestamp(new Timestamp(resultSet.getLong("timestamp") * 1000));
        transactionData.setTxId(resultSet.getString("txid"));
        transactionData.setAmount(resultSet.getBigDecimal("amount"));

        transactionDataList.add(transactionData);
      }

      resultSet.close();

      return transactionDataList;
    } catch (Exception e) {
      throw new DfxException("getYieldmachineCustomerTransactionDataList", e);
    }
  }

  /**
   * 
   */
  private class TransactionData {
    private boolean isDeposit = false;
    private Timestamp timestamp = null;
    private String txId = null;
    private BigDecimal amount = null;

    private BigDecimal totalBalance = null;

    private boolean isDeposit() {
      return isDeposit;
    }

    private void setDeposit(boolean isDeposit) {
      this.isDeposit = isDeposit;
    }

    private Timestamp getTimestamp() {
      return timestamp;
    }

    private void setTimestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
    }

    private String getTxId() {
      return txId;
    }

    private void setTxId(String txId) {
      this.txId = txId;
    }

    private BigDecimal getAmount() {
      return amount;
    }

    private void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    private BigDecimal getTotalBalance() {
      return totalBalance;
    }

    private void setTotalBalance(BigDecimal totalBalance) {
      this.totalBalance = totalBalance;
    }
  }
}
