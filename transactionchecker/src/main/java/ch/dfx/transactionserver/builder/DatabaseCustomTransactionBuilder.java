package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVinData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountInDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountOutDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.handler.DatabaseAddressHandler;

/**
 * 
 */
public class DatabaseCustomTransactionBuilder {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseCustomTransactionBuilder.class);

  // ...
  private static final String CUSTOM_TYPE_NONE = "0";
  private static final String CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS = "a";
  private static final String CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT = "B";
  private static final String CUSTOM_TYPE_UTXOS_TO_ACCOUNT = "U";

  private final Map<String, CustomTransactionMethod> customTransactionMethodMap;

  private interface CustomTransactionMethod {
    void fillCustomInfo(
        @Nonnull Integer typeNumber,
        @Nonnull DefiTransactionData transactionData,
        @Nonnull TransactionDTO transactionDTO) throws DfxException;
  }

  // ...
  private final NetworkEnum network;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseAddressHandler databaseAddressHandler;

  private final DefiDataProvider dataProvider;

  private final Map<String, Integer> customTypeCodeToNumberMap;

  /**
   * 
   */
  public DatabaseCustomTransactionBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseAddressHandler databaseAddressHandler) {
    this.network = network;
    this.databaseBlockHelper = databaseBlockHelper;
    this.databaseAddressHandler = databaseAddressHandler;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.customTransactionMethodMap = new HashMap<>();
    this.customTypeCodeToNumberMap = new LinkedHashMap<>();

    setup();
  }

  /**
   * 
   */
  private void setup() {
    LOGGER.trace("setup()");

    customTransactionMethodMap.put(
        CUSTOM_TYPE_ANY_ACCOUNTS_TO_ACCOUNTS,
        (typeNumber, transactionData, transactionDTO) -> fillCustomAnyAccountToAccountInfo(typeNumber, transactionData, transactionDTO));
    customTransactionMethodMap.put(
        CUSTOM_TYPE_ACCOUNT_TO_ACCOUNT,
        (typeNumber, transactionData, transactionDTO) -> fillCustomAccountToAccountInfo(typeNumber, transactionData, transactionDTO));
    customTransactionMethodMap.put(
        CUSTOM_TYPE_UTXOS_TO_ACCOUNT,
        (typeNumber, transactionData, transactionDTO) -> fillCustomUtxosToAccountInfo(typeNumber, transactionData, transactionDTO));
  }

  /**
   * 
   */
  public void fillCustomTypeCodeToNumberMap(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("fillCustomTypeCodeToNumberMap()");

    customTypeCodeToNumberMap.clear();

    try (Statement statement = connection.createStatement()) {
      String selectSql = "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".type";
      selectSql = DatabaseUtils.replaceSchema(network, selectSql);

      ResultSet resultSet = statement.executeQuery(selectSql);

      while (resultSet.next()) {
        customTypeCodeToNumberMap.put(resultSet.getString("type_code"), resultSet.getInt("number"));
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillCustomTypeCodeToNumberMap", e);
    }
  }

  /**
   * 
   */
  public void fillCustomTransactionInfo(
      @Nonnull DefiTransactionData transactionData,
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillCustomTransactionInfo()");

    Integer blockNumber = transactionDTO.getBlockNumber();
    String transactionId = transactionDTO.getTransactionId();

    transactionDTO.setCustomTypeCode(CUSTOM_TYPE_NONE);

    // ...
    Boolean isAppliedCustomTransaction =
        dataProvider.isAppliedCustomTransaction(transactionId, (long) blockNumber);

    if (BooleanUtils.isTrue(isAppliedCustomTransaction)) {
      byte customType = getCustomType(transactionData);

      if (0x00 != customType) {
        String typeCode = String.valueOf((char) customType);
        transactionDTO.setCustomTypeCode(typeCode);

        CustomTransactionMethod customTransactionMethod = customTransactionMethodMap.get(typeCode);

        if (null != customTransactionMethod) {
          Integer typeNumber = customTypeCodeToNumberMap.get(typeCode);

          if (null == typeNumber) {
            throw new DfxException("Unknown type '" + typeCode + "'");
          }

          customTransactionMethod.fillCustomInfo(typeNumber, transactionData, transactionDTO);
        }
      }
    }
  }

  /**
   * Return: 0x00 = No Custom Type
   */
  private byte getCustomType(@Nonnull DefiTransactionData transactionData) throws DfxException {
    byte customType = 0x00;

    for (DefiTransactionVoutData transactionVoutData : transactionData.getVout()) {
      DefiTransactionScriptPubKeyData transactionVoutScriptPubKeyData = transactionVoutData.getScriptPubKey();
      String scriptPubKeyHex = transactionVoutScriptPubKeyData.getHex();

      customType = dataProvider.getCustomType(scriptPubKeyHex);

      if (0x00 != customType) {
        break;
      }
    }

    return customType;
  }

  /**
   * 
   */
  private void fillCustomAnyAccountToAccountInfo(
      @Nonnull Integer typeNumber,
      @Nonnull DefiTransactionData transactionData,
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillCustomAnyAccountToAccountInfo()");

    // ...
    String hex = transactionData.getHex();
    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);
    Map<String, Object> resultMap = customData.getResults();

    // ...
    @SuppressWarnings("unchecked")
    Map<String, Object> fromMap = (Map<String, Object>) resultMap.get("from");

    @SuppressWarnings("unchecked")
    Map<String, Object> toMap = (Map<String, Object>) resultMap.get("to");

    // ...
    for (Entry<String, Object> fromMapEntry : fromMap.entrySet()) {
      String fromAddress = fromMapEntry.getKey();
      AddressDTO fromAddressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, fromAddress);

      String fromValue = (String) fromMapEntry.getValue();

      String[] fromTokenSplit = fromValue.split("\\,");

      for (String fromToken : fromTokenSplit) {
        String[] fromValueSplit = fromToken.split("\\@");

        if (2 == fromValueSplit.length) {
          BigDecimal fromAmount = new BigDecimal(fromValueSplit[0]);
          Integer fromTokenNumber = Integer.valueOf(fromValueSplit[1]);

          // ...
          TransactionCustomAccountToAccountInDTO customAccountToAccountInDTO =
              new TransactionCustomAccountToAccountInDTO(
                  transactionDTO.getBlockNumber(),
                  transactionDTO.getNumber(),
                  typeNumber,
                  fromAddressDTO.getNumber(),
                  fromTokenNumber);

          customAccountToAccountInDTO.setAmount(fromAmount);

          transactionDTO.addCustomAccountToAccountInDTO(customAccountToAccountInDTO);
        }
      }
    }

    // ...
    for (Entry<String, Object> toMapEntry : toMap.entrySet()) {
      String toAddress = toMapEntry.getKey();
      AddressDTO toAddressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, toAddress);

      String toValue = (String) toMapEntry.getValue();

      String[] toTokenSplit = toValue.split("\\,");

      for (String toToken : toTokenSplit) {
        String[] toValueSplit = toToken.split("\\@");

        if (2 == toValueSplit.length) {
          BigDecimal toAmount = new BigDecimal(toValueSplit[0]);
          Integer toTokenNumber = Integer.valueOf(toValueSplit[1]);

          // ...
          TransactionCustomAccountToAccountOutDTO customAccountToAccountOutDTO =
              new TransactionCustomAccountToAccountOutDTO(
                  transactionDTO.getBlockNumber(),
                  transactionDTO.getNumber(),
                  typeNumber,
                  toAddressDTO.getNumber(),
                  toTokenNumber);

          customAccountToAccountOutDTO.setAmount(toAmount);

          transactionDTO.addCustomAccountToAccountOutDTO(customAccountToAccountOutDTO);
        }
      }
    }
  }

  /**
   * 
   */
  private void fillCustomAccountToAccountInfo(
      @Nonnull Integer typeNumber,
      @Nonnull DefiTransactionData transactionData,
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillCustomAccountToAccountInfo()");

    // ...
    String hex = transactionData.getHex();
    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    Map<String, Object> resultMap = customData.getResults();

    @SuppressWarnings("unchecked")
    Map<String, Object> toMap = (Map<String, Object>) resultMap.get("to");
    Map<Integer, BigDecimal> outTokenToAmountMap = new LinkedHashMap<>();

    for (Entry<String, Object> toMapEntry : toMap.entrySet()) {
      String toAddress = toMapEntry.getKey();
      AddressDTO toAddressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, toAddress);

      String toValue = (String) toMapEntry.getValue();

      String[] toTokenSplit = toValue.split("\\,");

      for (String toToken : toTokenSplit) {
        String[] toValueSplit = toToken.split("\\@");

        if (2 == toValueSplit.length) {
          BigDecimal toAmount = new BigDecimal(toValueSplit[0]);
          Integer toTokenNumber = Integer.valueOf(toValueSplit[1]);

          // ...
          TransactionCustomAccountToAccountOutDTO customAccountToAccountOutDTO =
              new TransactionCustomAccountToAccountOutDTO(
                  transactionDTO.getBlockNumber(),
                  transactionDTO.getNumber(),
                  typeNumber,
                  toAddressDTO.getNumber(),
                  toTokenNumber);

          customAccountToAccountOutDTO.setAmount(toAmount);

          transactionDTO.addCustomAccountToAccountOutDTO(customAccountToAccountOutDTO);

          outTokenToAmountMap.merge(toTokenNumber, toAmount, (currVal, newVal) -> currVal.add(newVal));
        }
      }
    }

    // ...
    String fromAddress = (String) resultMap.get("from");
    AddressDTO fromAddressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, fromAddress);

    for (Entry<Integer, BigDecimal> outTokenToAmountMapEntry : outTokenToAmountMap.entrySet()) {
      TransactionCustomAccountToAccountInDTO customAccountToAccountInDTO =
          new TransactionCustomAccountToAccountInDTO(
              transactionDTO.getBlockNumber(),
              transactionDTO.getNumber(),
              typeNumber,
              fromAddressDTO.getNumber(),
              outTokenToAmountMapEntry.getKey());

      customAccountToAccountInDTO.setAmount(outTokenToAmountMapEntry.getValue());

      transactionDTO.addCustomAccountToAccountInDTO(customAccountToAccountInDTO);
    }
  }

  /**
   * 
   */
  private void fillCustomUtxosToAccountInfo(
      @Nonnull Integer typeNumber,
      @Nonnull DefiTransactionData transactionData,
      @Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillCustomUtxosToAccountInfo()");

    // ...
    String hex = transactionData.getHex();
    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    Map<String, Object> resultMap = customData.getResults();

    Set<String> resultMapToAddressSet = resultMap.keySet();

    BigDecimal fromAmount = BigDecimal.ZERO;

    for (String toAddress : resultMapToAddressSet) {
      AddressDTO toAddressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, toAddress);

      String toToken = (String) resultMap.get(toAddress);
      String[] toValueSplit = toToken.split("\\@");

      if (2 == toValueSplit.length) {
        BigDecimal toAmount = new BigDecimal(toValueSplit[0]);
        Integer toTokenNumber = Integer.valueOf(toValueSplit[1]);
        fromAmount = fromAmount.add(toAmount);

        // ...
        TransactionCustomAccountToAccountOutDTO customAccountToAccountOutDTO =
            new TransactionCustomAccountToAccountOutDTO(
                transactionDTO.getBlockNumber(),
                transactionDTO.getNumber(),
                typeNumber,
                toAddressDTO.getNumber(),
                toTokenNumber);

        customAccountToAccountOutDTO.setAmount(toAmount);

        transactionDTO.addCustomAccountToAccountOutDTO(customAccountToAccountOutDTO);
      }
    }

    // ...
    BigDecimal totalVout = BigDecimal.ZERO;

    for (DefiTransactionVoutData transactionVoutData : transactionData.getVout()) {
      totalVout = totalVout.add(transactionVoutData.getValue());
    }

    // ...
    List<String> inputAddressList = new ArrayList<>();

    BigDecimal totalVin = BigDecimal.ZERO;

    for (DefiTransactionVinData transactionVinData : transactionData.getVin()) {
      String vinTxid = transactionVinData.getTxid();
      Long vinVout = transactionVinData.getVout();

      DefiTransactionData vinTransactionData = dataProvider.getTransaction(vinTxid);
      DefiTransactionVoutData vinTransactionVoutData = vinTransactionData.getVout().get(vinVout.intValue());
      totalVin = totalVin.add(vinTransactionVoutData.getValue());

      DefiTransactionScriptPubKeyData transactionScriptPubKeyData = vinTransactionVoutData.getScriptPubKey();

      if (null != transactionScriptPubKeyData) {
        List<String> addressList = transactionScriptPubKeyData.getAddresses();

        if (null != addressList) {
          inputAddressList.addAll(addressList);
        }
      }
    }

    // ...
    int fromAddressNumber;

    if (inputAddressList.isEmpty()) {
      List<TransactionCustomAccountToAccountOutDTO> customAccountToAccountOutDTOList = transactionDTO.getCustomAccountToAccountOutDTOList();
      fromAddressNumber = customAccountToAccountOutDTOList.get(0).getAddressNumber();
    } else {
      AddressDTO fromAddressDTO = databaseAddressHandler.getAddressDTO(databaseBlockHelper, inputAddressList.get(0));
      fromAddressNumber = fromAddressDTO.getNumber();
    }

    // ...

    BigDecimal fee = totalVin.subtract(totalVout);
    fromAmount = fromAmount.add(fee);

    TransactionCustomAccountToAccountInDTO customAccountToAccountInDTO =
        new TransactionCustomAccountToAccountInDTO(
            transactionDTO.getBlockNumber(),
            transactionDTO.getNumber(),
            typeNumber,
            fromAddressNumber,
            0);

    customAccountToAccountInDTO.setAmount(fromAmount);

    transactionDTO.addCustomAccountToAccountInDTO(customAccountToAccountInDTO);
  }
}
