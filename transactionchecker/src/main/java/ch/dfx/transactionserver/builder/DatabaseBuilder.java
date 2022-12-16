package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
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
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.handler.DatabaseAddressHandler;

/**
 * 
 */
public class DatabaseBuilder {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseBuilder.class);

  // ...
  private PreparedStatement transactionOutSelectStatement = null;
  private PreparedStatement addressTransactionOutSelectStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;
  private final DatabaseAddressHandler databaseAddressHandler;

  private final DatabaseCustomTransactionBuilder customTransactionBuilder;

  private final DefiDataProvider dataProvider;

  // ...
  private int nextBlockNumber = 0;

  /**
   * 
   */
  public DatabaseBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseAddressHandler = new DatabaseAddressHandler(network);

    this.customTransactionBuilder =
        new DatabaseCustomTransactionBuilder(network, databaseBlockHelper, databaseAddressHandler);

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void build() throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    Long blockCount = dataProvider.getBlockCount();
    LOGGER.debug("[DatabaseBuilder] Block Count: " + blockCount);

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseBalanceHelper.openStatements(connection);
      openStatements(connection);

      // ...
      customTransactionBuilder.fillCustomTypeCodeToNumberMap(connection);
      databaseAddressHandler.setup(connection);

      // ...
      nextBlockNumber = DatabaseUtils.getNextBlockNumber(network, connection);

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
          LOGGER.debug("[DatabaseBuilder] Block: " + nextBlockNumber);

          BlockDTO blockDTO = createBlockDTO(connection);

          // ...
          Map<String, AddressDTO> newAddressMap = databaseAddressHandler.getNewAddressMap();
          databaseBlockHelper.saveAddress(newAddressMap);
          databaseAddressHandler.reset();

          databaseBlockHelper.saveBlock(blockDTO);
        }
      }

      closeStatements();
      databaseBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[DatabaseBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Address / Transaction / Out ...
      String transactionOutSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction WHERE txid=?";
      transactionOutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionOutSelectSql));

      String addressTransactionOutSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out WHERE block_number=? and transaction_number=? and vout_number=?";
      addressTransactionOutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionOutSelectSql));
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
      transactionOutSelectStatement.close();
      addressTransactionOutSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private BlockDTO createBlockDTO(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("createBlockDTO()");

    String blockHash = dataProvider.getBlockHash((long) nextBlockNumber);
    BlockDTO blockDTO = new BlockDTO(nextBlockNumber, blockHash);

    List<String> transactionIdList = getTransactionIdList(blockHash);

    for (int i = 0; i < transactionIdList.size(); i++) {
      String transactionId = transactionIdList.get(i);

      TransactionDTO transactionDTO = new TransactionDTO(nextBlockNumber, i, transactionId);

      DefiTransactionData transactionData = dataProvider.getTransaction(transactionId, blockHash);

      fillAddressList(transactionData, blockDTO, transactionDTO);

      customTransactionBuilder.fillCustomTransactionInfo(transactionData, transactionDTO);

      blockDTO.addTransactionDTO(transactionDTO);
    }

    nextBlockNumber++;

    return blockDTO;
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
      @Nonnull DefiTransactionData transactionData,
      @Nonnull BlockDTO blockDTO,
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillAddressList()");

    try {
      Integer blockNumber = transactionDTO.getBlockNumber();
      Integer transactionNumber = transactionDTO.getNumber();

      // ...
      List<DefiTransactionVinData> transactionVinDataList = transactionData.getVin();
      String coinbase = getTypeCoinbase(transactionVinDataList);

      // vout ...
      List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

      for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
        addAddressTransactionOut(blockNumber, transactionNumber, transactionVoutData, coinbase, transactionDTO);
      }

      // vin ...
      if (null == coinbase) {
        for (int i = 0; i < transactionVinDataList.size(); i++) {
          DefiTransactionVinData transactionVinData = transactionVinDataList.get(i);
          addAddressTransactionIn(blockNumber, transactionNumber, i, transactionVinData, blockDTO, transactionDTO);
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
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("addAddressTransactionOut()");

    DefiTransactionScriptPubKeyData transactionVoutScriptPubKeyData = transactionVoutData.getScriptPubKey();

    Integer index = transactionVoutData.getN().intValue();

    // ...
    List<String> addressList = transactionVoutScriptPubKeyData.getAddresses();

    if (null != addressList) {
      for (String address : addressList) {
        AddressDTO addressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, address);
        Integer addressNumber = addressDTO.getNumber();

        AddressTransactionOutDTO addressTransactionOutDTO =
            new AddressTransactionOutDTO(blockNumber, transactionNumber, index, addressNumber);

        addressTransactionOutDTO.setVout(transactionVoutData.getValue());

        if (null != coinbase) {
          addressTransactionOutDTO.setType(coinbase);
        }

        transactionDTO.addAddressTransactionOutDTO(addressTransactionOutDTO);
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
      @Nonnull BlockDTO blockDTO,
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("addAddressTransactionIn()");

    try {
      String outTxid = transactionVinData.getTxid();
      Long voutIndex = transactionVinData.getVout();

      AddressTransactionOutDTO addressTransactionOutDTO =
          getAddressTransactionOutDTOFromCurrentBlock(blockDTO, outTxid, voutIndex);

      if (null == addressTransactionOutDTO) {
        addressTransactionOutDTO = getAddressTransactionOutDTOFromPreviousBlock(outTxid, voutIndex);
      }

      // ...
      Integer addressNumber = addressTransactionOutDTO.getAddressNumber();
      Integer inBlockNumber = addressTransactionOutDTO.getBlockNumber();
      Integer inTransactionNumber = addressTransactionOutDTO.getTransactionNumber();

      AddressTransactionInDTO addressTransactionInDTO =
          new AddressTransactionInDTO(blockNumber, transactionNumber, inIndex, addressNumber, inBlockNumber, inTransactionNumber);

      addressTransactionInDTO.setVin(addressTransactionOutDTO.getVout());

      transactionDTO.addAddressTransactionInDTO(addressTransactionInDTO);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("addAddressTransactionIn", e);
    }
  }

  /**
   * 
   */
  private @Nullable AddressTransactionOutDTO getAddressTransactionOutDTOFromCurrentBlock(
      @Nonnull BlockDTO blockDTO,
      @Nonnull String outTxid,
      @Nonnull Long voutIndex) {
    LOGGER.trace("getAddressTransactionOutDTOFromCurrentBlock()");

    AddressTransactionOutDTO addressTransactionOutDTO = null;

    Optional<TransactionDTO> optionalTransactionDTO =
        blockDTO.getTransactionDTOList().stream().filter(data -> outTxid.equals(data.getTransactionId())).findFirst();

    if (optionalTransactionDTO.isPresent()) {
      TransactionDTO transactionDTO = optionalTransactionDTO.get();

      Optional<AddressTransactionOutDTO> optionalAddressTransactionOutDTO =
          transactionDTO.getAddressTransactionOutDTOList().stream().filter(data -> voutIndex.intValue() == data.getVoutNumber()).findFirst();

      if (optionalAddressTransactionOutDTO.isPresent()) {
        addressTransactionOutDTO = optionalAddressTransactionOutDTO.get();
      }
    }

    return addressTransactionOutDTO;
  }

  /**
   * 
   */
  private AddressTransactionOutDTO getAddressTransactionOutDTOFromPreviousBlock(
      @Nonnull String outTxid,
      @Nonnull Long voutIndex) throws DfxException {
    LOGGER.trace("getAddressTransactionOutDTOFromPreviousBlock()");

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

      AddressTransactionOutDTO addressTransactionOutDTO =
          new AddressTransactionOutDTO(inBlockNumber, inTransactionNumber, voutIndex.intValue(), addressNumber);
      addressTransactionOutDTO.setVout(vout);

      return addressTransactionOutDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressTransactionOutDTOFromPreviousBlock", e);
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
}
