package ch.dfx.transactionserver.builder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVinData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.AddressTransactionInDTO;
import ch.dfx.transactionserver.data.AddressTransactionOutDTO;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class DatabaseBuilder {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseBuilder.class);

  private PreparedStatement blockInsertStatement = null;
  private PreparedStatement transactionInsertStatement = null;
  private PreparedStatement addressInsertStatement = null;

  private PreparedStatement addressTransactionOutInsertStatement = null;
  private PreparedStatement addressTransactionInInsertStatement = null;

  private PreparedStatement addressSelectStatement = null;
  private PreparedStatement transactionOutSelectStatement = null;
  private PreparedStatement addressTransactionOutSelectStatement = null;

  // ...
  private final H2DBManager databaseManager;
  private final DefiDataProvider dataProvider;

  private final Map<String, AddressDTO> newAddressMap;

  private int nextBlockNumber = 0;
  private int nextAddressNumber = 0;

  /**
   * 
   */
  public DatabaseBuilder(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.newAddressMap = new LinkedHashMap<>();
  }

  /**
   * 
   */
  public void build() throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    Long blockCount = dataProvider.getBlockCount();
    LOGGER.debug("Block Count: " + blockCount);

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();
      openStatements(connection);

      // ...
      nextBlockNumber = getNextBlockNumber(connection);
      nextAddressNumber = getNextAddressNumber(connection);

      // ...
      int syncLoop = Integer.parseInt(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.H2_SYNC_LOOP));
      int syncCommit = Integer.parseInt(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.H2_SYNC_COMMIT));
      LOGGER.debug("Sync Loop:   " + syncLoop);
      LOGGER.debug("Sync Commit: " + syncCommit);

      // ...
      for (int i = 0; i < syncLoop; i++) {
        if (0 == nextBlockNumber % syncCommit) {
          connection.commit();
        }

        if (nextBlockNumber <= blockCount) {
          LOGGER.debug("Block: " + nextBlockNumber);

          BlockDTO cacheBlockData = createCacheBlockData(connection);

          saveAddress();
          saveBlock(cacheBlockData);
        }
      }

      closeStatements();

      connection.commit();
    } catch (DfxException e) {

      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Block ...
      String blockInsertSql = "INSERT INTO public.block (number, hash) VALUES (?, ?)";
      blockInsertStatement = connection.prepareStatement(blockInsertSql);

      // Transaction ...
      String transactionInsertSql = "INSERT INTO public.transaction (block_number, number, txid) VALUES (?, ?, ?)";
      transactionInsertStatement = connection.prepareStatement(transactionInsertSql);

      // Address ...
      String addressInsertSql = "INSERT INTO public.address (number, address, hex) VALUES (?, ?, ?)";
      addressInsertStatement = connection.prepareStatement(addressInsertSql);

      // Address / Transaction / Out ...
      String addressTransactionOutInsertSql =
          "INSERT INTO public.address_transaction_out"
              + " (block_number, transaction_number, vout_number, address_number, vout, type)"
              + " VALUES (?, ?, ?, ?, ?, ?)";
      addressTransactionOutInsertStatement = connection.prepareStatement(addressTransactionOutInsertSql);

      // Address / Transaction / In ...
      String addressTransactionInInsertSql =
          "INSERT INTO public.address_transaction_in"
              + " (block_number, transaction_number, vin_number, address_number, in_block_number, in_transaction_number, vin)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?)";
      addressTransactionInInsertStatement = connection.prepareStatement(addressTransactionInInsertSql);

      // Address ...
      String addressSelectSql = "SELECT * FROM public.address WHERE address=?";
      addressSelectStatement = connection.prepareStatement(addressSelectSql);

      // Address / Transaction / Out ...
      String transactionOutSelectSql = "SELECT * FROM public.transaction WHERE txid=?";
      transactionOutSelectStatement = connection.prepareStatement(transactionOutSelectSql);

      String addressTransactionOutSelectSql = "SELECT * FROM public.address_transaction_out WHERE block_number=? and transaction_number=? and vout_number=?";
      addressTransactionOutSelectStatement = connection.prepareStatement(addressTransactionOutSelectSql);
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
      blockInsertStatement.close();
      transactionInsertStatement.close();
      addressInsertStatement.close();

      addressTransactionOutInsertStatement.close();
      addressTransactionInInsertStatement.close();

      addressSelectStatement.close();
      transactionOutSelectStatement.close();
      addressTransactionOutSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private BlockDTO createCacheBlockData(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("createCacheBlockData()");

    String blockHash = dataProvider.getBlockHash((long) nextBlockNumber);
    BlockDTO cacheBlockData = new BlockDTO(nextBlockNumber, blockHash);

    List<String> transactionIdList = getTransactionIdList(blockHash);

    for (int i = 0; i < transactionIdList.size(); i++) {
      String transactionId = transactionIdList.get(i);

      TransactionDTO cacheTransactionData = new TransactionDTO(nextBlockNumber, i, transactionId);

      fillAddressList(blockHash, transactionId, cacheBlockData, cacheTransactionData);

      cacheBlockData.addTransactionDTO(cacheTransactionData);
    }

    nextBlockNumber++;

    return cacheBlockData;
  }

  /**
   * 
   */
  private List<String> getTransactionIdList(@Nonnull String blockHash) throws DfxException {
    LOGGER.trace("getTransactionIdList()");

    try {
      List<String> transactionIdList = new ArrayList<>();

      DefiBlockData blockData = dataProvider.getBlock(blockHash);

      for (String transactionId : blockData.getTx()) {
        transactionIdList.add(transactionId);
      }

      return transactionIdList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getTransactionIdList", e);
    }
  }

  /**
   * 
   */
  private void fillAddressList(
      @Nonnull String blockHash,
      @Nonnull String transactionId,
      @Nonnull BlockDTO cacheBlockData,
      @Nonnull TransactionDTO cacheTransactionData) throws DfxException {
    LOGGER.trace("fillAddressList()");

    try {
      Integer blockNumber = cacheTransactionData.getBlockNumber();
      Integer transactionNumber = cacheTransactionData.getNumber();

      DefiTransactionData transactionData = dataProvider.getTransaction(transactionId, blockHash);

      // ...
      List<DefiTransactionVinData> transactionVinDataList = transactionData.getVin();
      String coinbase = getTypeCoinbase(transactionVinDataList);

      // vout ...
      List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

      for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
        addAddressTransactionOut(blockNumber, transactionNumber, transactionVoutData, coinbase, cacheTransactionData);
      }

      // vin ...
      if (null == coinbase) {
        for (int i = 0; i < transactionVinDataList.size(); i++) {
          DefiTransactionVinData transactionVinData = transactionVinDataList.get(i);
          addAddressTransactionIn(blockNumber, transactionNumber, i, transactionVinData, cacheBlockData, cacheTransactionData);
        }
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("fillAddressList", e);
    }
  }

  /**
   * 
   */
  private void addAddressTransactionOut(
      @Nonnull Integer blockNumber,
      @Nonnull Integer transactionNumber,
      @Nonnull DefiTransactionVoutData transactionVoutData,
      @Nullable String coinbase,
      @Nonnull TransactionDTO cacheTransactionData) throws DfxException {
    LOGGER.trace("addAddressTransactionOut()");

    DefiTransactionScriptPubKeyData transactionVoutScriptPubKeyData = transactionVoutData.getScriptPubKey();

    Integer index = transactionVoutData.getN().intValue();
    String hex = transactionVoutScriptPubKeyData.getHex();

    List<String> addressList = transactionVoutScriptPubKeyData.getAddresses();

    if (null != addressList) {
      for (String address : addressList) {
        AddressDTO cacheAddressData = getCacheAddressData(address, hex);
        Integer addressNumber = cacheAddressData.getNumber();

        AddressTransactionOutDTO cacheAddressTransactionOutData =
            new AddressTransactionOutDTO(blockNumber, transactionNumber, index, addressNumber);

        cacheAddressTransactionOutData.setVout(transactionVoutData.getValue());

        if (null != coinbase) {
          cacheAddressTransactionOutData.setType(coinbase);
        }

        cacheTransactionData.addAddressTransactionOutDTO(cacheAddressTransactionOutData);
      }
    }
  }

  /**
   * 
   */
  private void addAddressTransactionIn(
      @Nonnull Integer blockNumber,
      @Nonnull Integer transactionNumber,
      @Nonnull Integer inIndex,
      @Nonnull DefiTransactionVinData transactionVinData,
      @Nonnull BlockDTO cacheBlockData,
      @Nonnull TransactionDTO cacheTransactionData) throws DfxException {
    LOGGER.trace("addAddressTransactionIn()");

    try {
      String outTxid = transactionVinData.getTxid();
      Long voutIndex = transactionVinData.getVout();

      AddressTransactionOutDTO cacheAddressTransactionOutData =
          getCacheAddressTransactionOutDataFromCurrentBlock(cacheBlockData, outTxid, voutIndex);

      if (null == cacheAddressTransactionOutData) {
        cacheAddressTransactionOutData = getCacheAddressTransactionOutDataFromPreviousBlock(outTxid, voutIndex);
      }

      // ...
      Integer addressNumber = cacheAddressTransactionOutData.getAddressNumber();
      Integer inBlockNumber = cacheAddressTransactionOutData.getBlockNumber();
      Integer inTransactionNumber = cacheAddressTransactionOutData.getTransactionNumber();

      AddressTransactionInDTO cacheAddressTransactionInData =
          new AddressTransactionInDTO(blockNumber, transactionNumber, inIndex, addressNumber, inBlockNumber, inTransactionNumber);

      cacheAddressTransactionInData.setVin(cacheAddressTransactionOutData.getVout());

      cacheTransactionData.addAddressTransactionInDTO(cacheAddressTransactionInData);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("addAddressTransactionIn", e);
    }
  }

  /**
   * 
   */
  private @Nullable AddressTransactionOutDTO getCacheAddressTransactionOutDataFromCurrentBlock(
      @Nonnull BlockDTO cacheBlockData,
      @Nonnull String outTxid,
      @Nonnull Long voutIndex) {
    LOGGER.trace("getCacheAddressTransactionOutDataFromCurrentBlock()");

    AddressTransactionOutDTO cacheAddressTransactionOutData = null;

    Optional<TransactionDTO> optionalCacheTransactionData =
        cacheBlockData.getTransactionDTOList().stream().filter(data -> outTxid.equals(data.getTransactionId())).findFirst();

    if (optionalCacheTransactionData.isPresent()) {
      TransactionDTO cacheTransactionData = optionalCacheTransactionData.get();

      Optional<AddressTransactionOutDTO> optionalCacheAddressTransactionOutData =
          cacheTransactionData.getAddressTransactionOutDTOList().stream().filter(data -> voutIndex.intValue() == data.getVoutNumber().intValue()).findFirst();

      if (optionalCacheAddressTransactionOutData.isPresent()) {
        cacheAddressTransactionOutData = optionalCacheAddressTransactionOutData.get();
      }
    }

    return cacheAddressTransactionOutData;
  }

  /**
   * 
   */
  private AddressTransactionOutDTO getCacheAddressTransactionOutDataFromPreviousBlock(
      @Nonnull String outTxid,
      @Nonnull Long voutIndex) throws DfxException {
    LOGGER.trace("getCacheAddressTransactionOutDataFromPreviousBlock()");

    try {
      transactionOutSelectStatement.setString(1, outTxid);

      ResultSet resultSet1 = transactionOutSelectStatement.executeQuery();

      if (!resultSet1.next()) {
        throw new DfxException("No Transaction: " + outTxid);
      }

      int inBlockNumber = resultSet1.getInt(1);
      int inTransactionNumber = resultSet1.getInt(2);

      resultSet1.close();

      // ...
      addressTransactionOutSelectStatement.setInt(1, inBlockNumber);
      addressTransactionOutSelectStatement.setInt(2, inTransactionNumber);
      addressTransactionOutSelectStatement.setInt(3, voutIndex.intValue());

      ResultSet resultSet2 = addressTransactionOutSelectStatement.executeQuery();

      if (!resultSet2.next()) {
        throw new DfxException("No Transaction: " + inBlockNumber + " / " + inTransactionNumber + " / " + voutIndex);
      }

      int addressNumber = resultSet2.getInt(4);
      BigDecimal vout = resultSet2.getBigDecimal(5);

      resultSet2.close();

      AddressTransactionOutDTO cacheAddressTransactionOutData =
          new AddressTransactionOutDTO(inBlockNumber, inTransactionNumber, voutIndex.intValue(), addressNumber);
      cacheAddressTransactionOutData.setVout(vout);

      return cacheAddressTransactionOutData;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getCacheAddressTransactionOutDataFromPreviousBlock", e);
    }
  }

  /**
   * 
   */
  private AddressDTO getCacheAddressData(
      @Nonnull String address,
      @Nonnull String hex) throws DfxException {
    LOGGER.trace("getCacheAddressData()");

    AddressDTO cacheAddressData = readAddress(address);

    if (null == cacheAddressData) {
      cacheAddressData = newAddressMap.get(address);
    }

    if (null == cacheAddressData) {
      cacheAddressData = new AddressDTO(nextAddressNumber++, address, hex);
      newAddressMap.put(address, cacheAddressData);
    }

    return cacheAddressData;
  }

  /**
   * 
   */
  private AddressDTO readAddress(@Nonnull String address) throws DfxException {
    LOGGER.trace("readAddress()");

    try {
      AddressDTO cacheAddressData = null;

      addressSelectStatement.setString(1, address);

      ResultSet resultSet = addressSelectStatement.executeQuery();

      if (resultSet.next()) {
        int addressNumber = resultSet.getInt(1);
        // String address = resultSet.getString(2);
        String hex = resultSet.getString(3);

        cacheAddressData = new AddressDTO(addressNumber, address, hex);
      }

      resultSet.close();

      return cacheAddressData;
    } catch (Exception e) {
      throw new DfxException("readAddress", e);
    }
  }

  /**
   * 
   */
  private @Nullable String getTypeCoinbase(@Nonnull List<DefiTransactionVinData> transactionVinDataList) {
    LOGGER.trace("getTypeCoinbase()");

    String typeCoinbase = null;

    if (1 == transactionVinDataList.size()) {
      DefiTransactionVinData transactionVinData = transactionVinDataList.get(0);

      typeCoinbase = (null == transactionVinData.getCoinbase() ? null : "coinbase");
    }

    return typeCoinbase;
  }

  /**
   * 
   */
  private int getNextBlockNumber(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getNextBlockNumber()");

    String sqlSelect =
        new StringBuilder()
            .append("SELECT MAX(number) FROM public.block")
            .toString();

    return getNextNumber(connection, sqlSelect);
  }

  /**
   * 
   */
  private int getNextAddressNumber(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("getNextAddressNumber()");

    String sqlSelect =
        new StringBuilder()
            .append("SELECT MAX(number) FROM public.address")
            .toString();

    return getNextNumber(connection, sqlSelect);
  }

  /**
   * 
   */
  private int getNextNumber(
      @Nonnull Connection connection,
      @Nonnull String sqlSelect) throws DfxException {
    LOGGER.trace("getNextNumber()");

    try (Statement statement = connection.createStatement()) {
      int nextNumber = -1;

      ResultSet resultSet = statement.executeQuery(sqlSelect);

      if (resultSet.next()) {
        nextNumber = resultSet.getInt(1);

        if (!resultSet.wasNull()) {
          nextNumber++;
        }
      }

      resultSet.close();

      if (-1 == nextNumber) {
        throw new DfxException("next number not found ...");
      }

      return nextNumber;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getNextNumber", e);
    }
  }

  /**
   * 
   */
  private void saveAddress() throws DfxException {
    LOGGER.trace("saveAddress()");

    try {
      for (AddressDTO cacheAddressData : newAddressMap.values()) {
        addressInsertStatement.setLong(1, cacheAddressData.getNumber());
        addressInsertStatement.setString(2, cacheAddressData.getAddress());
        addressInsertStatement.setString(3, cacheAddressData.getHex());
        addressInsertStatement.execute();
      }

      newAddressMap.clear();
    } catch (Exception e) {
      throw new DfxException("saveAddress", e);
    }
  }

  /**
   * 
   */
  private void saveBlock(@Nonnull BlockDTO cacheBlockData) throws DfxException {
    LOGGER.trace("saveBlock()");

    try {
      Integer blockNumber = cacheBlockData.getNumber();

      blockInsertStatement.setInt(1, blockNumber);
      blockInsertStatement.setString(2, cacheBlockData.getHash());
      blockInsertStatement.execute();

      for (TransactionDTO cacheTransactionData : cacheBlockData.getTransactionDTOList()) {
        saveTransaction(cacheTransactionData);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("saveBlock", e);
    }
  }

  /**
   * 
   */
  private void saveTransaction(
      @Nonnull TransactionDTO cacheTransactionData) throws DfxException {
    LOGGER.trace("saveTransaction()");

    try {
      Integer blockNumber = cacheTransactionData.getBlockNumber();
      Integer transactionNumber = cacheTransactionData.getNumber();

      transactionInsertStatement.setLong(1, blockNumber);
      transactionInsertStatement.setLong(2, transactionNumber);
      transactionInsertStatement.setString(3, cacheTransactionData.getTransactionId());
      transactionInsertStatement.execute();

      for (AddressTransactionOutDTO cacheAddressTransactionOutData : cacheTransactionData.getAddressTransactionOutDTOList()) {
        saveAddressTransactionOut(cacheAddressTransactionOutData);
      }

      for (AddressTransactionInDTO cacheAddressTransactionInData : cacheTransactionData.getAddressTransactionInDTOList()) {
        saveAddressTransactionIn(cacheAddressTransactionInData);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("saveTransaction", e);
    }
  }

  /**
   * 
   */
  private void saveAddressTransactionOut(@Nonnull AddressTransactionOutDTO cacheAddressTransactionOutData) throws DfxException {
    LOGGER.trace("saveAddressTransactionOut()");

    try {
      addressTransactionOutInsertStatement.setInt(1, cacheAddressTransactionOutData.getBlockNumber());
      addressTransactionOutInsertStatement.setInt(2, cacheAddressTransactionOutData.getTransactionNumber());
      addressTransactionOutInsertStatement.setInt(3, cacheAddressTransactionOutData.getVoutNumber());
      addressTransactionOutInsertStatement.setInt(4, cacheAddressTransactionOutData.getAddressNumber());

      addressTransactionOutInsertStatement.setBigDecimal(5, cacheAddressTransactionOutData.getVout());
      addressTransactionOutInsertStatement.setString(6, cacheAddressTransactionOutData.getType());
      addressTransactionOutInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("saveAddressTransactionOut", e);
    }
  }

  /**
   * 
   */
  private void saveAddressTransactionIn(@Nonnull AddressTransactionInDTO cacheAddressTransactionInData) throws DfxException {
    LOGGER.trace("saveAddressTransactionIn()");

    try {
      addressTransactionInInsertStatement.setInt(1, cacheAddressTransactionInData.getBlockNumber());
      addressTransactionInInsertStatement.setInt(2, cacheAddressTransactionInData.getTransactionNumber());
      addressTransactionInInsertStatement.setInt(3, cacheAddressTransactionInData.getVinNumber());
      addressTransactionInInsertStatement.setInt(4, cacheAddressTransactionInData.getAddressNumber());

      addressTransactionInInsertStatement.setInt(5, cacheAddressTransactionInData.getInBlockNumber());
      addressTransactionInInsertStatement.setInt(6, cacheAddressTransactionInData.getInTransactionNumber());

      addressTransactionInInsertStatement.setBigDecimal(7, cacheAddressTransactionInData.getVin());
      addressTransactionInInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("saveAddressTransactionIn", e);
    }
  }
}
