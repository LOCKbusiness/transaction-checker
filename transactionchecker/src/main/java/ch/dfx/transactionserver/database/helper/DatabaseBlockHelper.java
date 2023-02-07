package ch.dfx.transactionserver.database.helper;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.AddressTransactionInDTO;
import ch.dfx.transactionserver.data.AddressTransactionOutDTO;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountInDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountOutDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;

/**
 * 
 */
public class DatabaseBlockHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseBlockHelper.class);

  // ...
  private PreparedStatement blockByNumberSelectStatement = null;
  private PreparedStatement blockInsertStatement = null;

  private PreparedStatement transactionByBlockNumberSelectStatement = null;
  private PreparedStatement transactionByIdSelectStatement = null;
  private PreparedStatement transactionInsertStatement = null;

  private PreparedStatement addressTransactionInByBlockNumberSelectStatement = null;
  private PreparedStatement addressTransactionOutByBlockNumberSelectStatement = null;

  private PreparedStatement addressTransactionInByBlockAndTransactionSelectStatement = null;
  private PreparedStatement addressTransactionOutByBlockAndTransactionSelectStatement = null;

  private PreparedStatement customTransactionAccountToAccountInByBlockNumberSelectStatement = null;
  private PreparedStatement customTransactionAccountToAccountInInsertStatement = null;

  private PreparedStatement customTransactionAccountToAccountOutByBlockNumberSelectStatement = null;
  private PreparedStatement customTransactionAccountToAccountOutInsertStatement = null;

  private PreparedStatement addressByNumberSelectStatement = null;
  private PreparedStatement addressByAddressSelectStatement = null;
  private PreparedStatement addressInsertStatement = null;

  private PreparedStatement addressTransactionOutInsertStatement = null;
  private PreparedStatement addressTransactionInInsertStatement = null;

  private PreparedStatement masternodeSelectStatement = null;
  private PreparedStatement masternodeWhitelistByOwnerAddressSelectStatement = null;

  // ...
  private final NetworkEnum network;

  /**
   * 
   */
  public DatabaseBlockHelper(@Nonnull NetworkEnum network) {
    this.network = network;
  }

  /**
   * 
   */
  public void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Block ...
      String blockByNumberSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".block WHERE number=?";
      blockByNumberSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, blockByNumberSelectSql));

      String blockInsertSql = "INSERT INTO " + TOKEN_PUBLIC_SCHEMA + ".block (number, hash, timestamp) VALUES (?, ?, ?)";
      blockInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, blockInsertSql));

      // Transaction ...
      String transactionByBlockNumberSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction WHERE block_number=?";
      transactionByBlockNumberSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionByBlockNumberSelectSql));

      String transactionByIdSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".transaction WHERE txid=?";
      transactionByIdSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionByIdSelectSql));

      String transactionInsertSql = "INSERT INTO " + TOKEN_PUBLIC_SCHEMA + ".transaction (block_number, number, txid, custom_type_code) VALUES (?, ?, ?, ?)";
      transactionInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionInsertSql));

      // Address Transaction In / Out ...
      String addressTransactionInByBlockNumberSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in WHERE block_number=?";
      addressTransactionInByBlockNumberSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionInByBlockNumberSelectSql));

      String addressTransactionInByBlockAndTransactionSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in WHERE block_number=? AND transaction_number=?";
      addressTransactionInByBlockAndTransactionSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionInByBlockAndTransactionSelectSql));

      String addressTransactionInInsertSql =
          "INSERT INTO " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in"
              + " (block_number, transaction_number, vin_number, address_number, in_block_number, in_transaction_number, vin)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?)";
      addressTransactionInInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionInInsertSql));

      String addressTransactionOutByBlockNumberSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out WHERE block_number=?";
      addressTransactionOutByBlockNumberSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionOutByBlockNumberSelectSql));

      String addressTransactionOutByBlockAndTransactionSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out WHERE block_number=? AND transaction_number=?";
      addressTransactionOutByBlockAndTransactionSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionOutByBlockAndTransactionSelectSql));

      String addressTransactionOutInsertSql =
          "INSERT INTO " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out"
              + " (block_number, transaction_number, vout_number, address_number, vout, type)"
              + " VALUES (?, ?, ?, ?, ?, ?)";
      addressTransactionOutInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionOutInsertSql));

      // Custom Transaction AccountToAccount In / Out ...
      String customTransactionAccountToAccountInByBlockNumberSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in WHERE block_number=?";
      customTransactionAccountToAccountInByBlockNumberSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customTransactionAccountToAccountInByBlockNumberSelectSql));

      String customTransactionAccountToAccountInInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in"
              + " (block_number, transaction_number, type_number, address_number, amount, token_number)"
              + " VALUES (?, ?, ?, ?, ?, ?)";
      customTransactionAccountToAccountInInsertStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customTransactionAccountToAccountInInsertSql));

      String customTransactionAccountToAccountOutByBlockNumberSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out WHERE block_number=?";
      customTransactionAccountToAccountOutByBlockNumberSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customTransactionAccountToAccountOutByBlockNumberSelectSql));

      String customTransactionAccountToAccountOutInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out"
              + " (block_number, transaction_number, type_number, address_number, amount, token_number)"
              + " VALUES (?, ?, ?, ?, ?, ?)";
      customTransactionAccountToAccountOutInsertStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customTransactionAccountToAccountOutInsertSql));

      // Address ...
      String addressByNumberSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address WHERE number=?";
      addressByNumberSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressByNumberSelectSql));

      String addressByAddressSelectSql = "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address WHERE address=?";
      addressByAddressSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressByAddressSelectSql));

      String addressInsertSql = "INSERT INTO " + TOKEN_PUBLIC_SCHEMA + ".address (number, address) VALUES (?, ?)";
      addressInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressInsertSql));

      // Masternode ...
      String masternodeSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".masternode_whitelist";
      masternodeSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, masternodeSelectSql));

      String masternodeWhitelistByOwnerAddressSelectSql =
          masternodeSelectSql
              + " WHERE owner_address=?";
      masternodeWhitelistByOwnerAddressSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, masternodeWhitelistByOwnerAddressSelectSql));
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  public void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements()");

    try {
      blockByNumberSelectStatement.close();
      blockInsertStatement.close();

      transactionByBlockNumberSelectStatement.close();
      transactionByIdSelectStatement.close();
      transactionInsertStatement.close();

      addressTransactionInByBlockNumberSelectStatement.close();
      addressTransactionInInsertStatement.close();

      addressTransactionOutByBlockNumberSelectStatement.close();
      addressTransactionOutInsertStatement.close();

      customTransactionAccountToAccountInByBlockNumberSelectStatement.close();
      customTransactionAccountToAccountInInsertStatement.close();

      customTransactionAccountToAccountOutByBlockNumberSelectStatement.close();
      customTransactionAccountToAccountOutInsertStatement.close();

      addressByNumberSelectStatement.close();
      addressByAddressSelectStatement.close();
      addressInsertStatement.close();

      masternodeSelectStatement.close();
      masternodeWhitelistByOwnerAddressSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public @Nullable BlockDTO getBlockDTOByNumber(int blockNumber) throws DfxException {
    LOGGER.trace("getBlockDTOByNumber()");

    try {
      BlockDTO blockDTO = null;

      blockByNumberSelectStatement.setInt(1, blockNumber);

      ResultSet resultSet = blockByNumberSelectStatement.executeQuery();

      if (resultSet.next()) {
        blockDTO =
            new BlockDTO(
                resultSet.getInt("number"),
                resultSet.getString("hash"),
                resultSet.getLong("timestamp"));
      }

      resultSet.close();

      return blockDTO;
    } catch (Exception e) {
      throw new DfxException("getBlockDTOByNumber", e);
    }
  }

  /**
   * 
   */
  public @Nullable BlockDTO getBlockDTOWithTransactionByNumber(int blockNumber) throws DfxException {
    LOGGER.trace("getBlockDTOWithTransactionByNumber()");

    try {
      BlockDTO blockDTO = getBlockDTOByNumber(blockNumber);

      if (null != blockDTO) {
        fillTransactionDTO(blockDTO);
        blockDTO.keepInternalState();
      }

      return blockDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getBlockDTOWithTransactionByNumber", e);
    }
  }

  /**
   * 
   */
  private void fillTransactionDTO(@Nonnull BlockDTO blockDTO) throws DfxException {
    LOGGER.trace("fillTransactionDTO()");

    try {
      transactionByBlockNumberSelectStatement.setInt(1, blockDTO.getNumber());

      ResultSet resultSet = transactionByBlockNumberSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionDTO transactionDTO =
            new TransactionDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("number"),
                resultSet.getString("txid"));
        transactionDTO.setCustomTypeCode(resultSet.getString("custom_type_code"));

        blockDTO.addTransactionDTO(transactionDTO);
      }

      for (TransactionDTO transactionDTO : blockDTO.getTransactionDTOList()) {
        fillAddressTransactionInDTO(transactionDTO);
        fillAddressTransactionOutDTO(transactionDTO);

        fillCustomTransactionAccountToAccountInDTO(transactionDTO);
        fillCustomTransactionAccountToAccountOutDTO(transactionDTO);

        transactionDTO.keepInternalState();
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillTransactionDTO", e);
    }
  }

  /**
   * 
   */
  private void fillAddressTransactionInDTO(@Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillAddressTransactionInDTO()");

    try {
      addressTransactionInByBlockNumberSelectStatement.setInt(1, transactionDTO.getBlockNumber());

      ResultSet resultSet = addressTransactionInByBlockNumberSelectStatement.executeQuery();

      while (resultSet.next()) {
        AddressTransactionInDTO addressTransactionInDTO =
            new AddressTransactionInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vin_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("in_block_number"),
                resultSet.getInt("in_transaction_number"));
        addressTransactionInDTO.setVin(resultSet.getBigDecimal("vin"));

        addressTransactionInDTO.keepInternalState();

        transactionDTO.addAddressTransactionInDTO(addressTransactionInDTO);
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillAddressTransactionInDTO", e);
    }
  }

  /**
   * 
   */
  private void fillAddressTransactionOutDTO(@Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillAddressTransactionOutDTO()");

    try {
      addressTransactionOutByBlockNumberSelectStatement.setInt(1, transactionDTO.getBlockNumber());

      ResultSet resultSet = addressTransactionOutByBlockNumberSelectStatement.executeQuery();

      while (resultSet.next()) {
        AddressTransactionOutDTO addressTransactionOutDTO =
            new AddressTransactionOutDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vout_number"),
                resultSet.getInt("address_number"));
        addressTransactionOutDTO.setVout(resultSet.getBigDecimal("vout"));

        addressTransactionOutDTO.keepInternalState();

        transactionDTO.addAddressTransactionOutDTO(addressTransactionOutDTO);
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillAddressTransactionOutDTO", e);
    }
  }

  /**
   * 
   */
  private void fillCustomTransactionAccountToAccountInDTO(@Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillCustomTransactionAccountToAccountInDTO()");
    try {
      customTransactionAccountToAccountInByBlockNumberSelectStatement.setInt(1, transactionDTO.getBlockNumber());

      ResultSet resultSet = customTransactionAccountToAccountInByBlockNumberSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionCustomAccountToAccountInDTO transactionCustomAccountToAccountInDTO =
            new TransactionCustomAccountToAccountInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("type_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("token_number"));
        transactionCustomAccountToAccountInDTO.setAmount(resultSet.getBigDecimal("amount"));

        transactionCustomAccountToAccountInDTO.keepInternalState();

        transactionDTO.addCustomAccountToAccountInDTO(transactionCustomAccountToAccountInDTO);
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillCustomTransactionAccountToAccountInDTO", e);
    }
  }

  /**
   * 
   */
  private void fillCustomTransactionAccountToAccountOutDTO(@Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("fillCustomTransactionAccountToAccountOutDTO()");
    try {
      customTransactionAccountToAccountOutByBlockNumberSelectStatement.setInt(1, transactionDTO.getBlockNumber());

      ResultSet resultSet = customTransactionAccountToAccountOutByBlockNumberSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionCustomAccountToAccountOutDTO transactionCustomAccountToAccountOutDTO =
            new TransactionCustomAccountToAccountOutDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("type_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("token_number"));
        transactionCustomAccountToAccountOutDTO.setAmount(resultSet.getBigDecimal("amount"));

        transactionCustomAccountToAccountOutDTO.keepInternalState();

        transactionDTO.addCustomAccountToAccountOutDTO(transactionCustomAccountToAccountOutDTO);
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillCustomTransactionAccountToAccountInDTO", e);
    }
  }

  /**
   * 
   */
  public @Nullable TransactionDTO getTransactionDTOById(@Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getTransactionDTOById()");

    try {
      TransactionDTO transactionDTO = null;

      transactionByIdSelectStatement.setString(1, transactionId);

      ResultSet resultSet = transactionByIdSelectStatement.executeQuery();

      if (resultSet.next()) {
        transactionDTO =
            new TransactionDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("number"),
                resultSet.getString("txid"));
        transactionDTO.setCustomTypeCode(resultSet.getString("custom_type_code"));

        transactionDTO.keepInternalState();
      }

      resultSet.close();

      return transactionDTO;
    } catch (Exception e) {
      throw new DfxException("getTransactionDTOById", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressDTO getAddressDTOByNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getAddressDTOByNumber()");

    try {
      addressByNumberSelectStatement.setInt(1, addressNumber);

      return getAddressDTO(addressByNumberSelectStatement);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressDTOByNumber", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressDTO getAddressDTOByAddress(@Nonnull String address) throws DfxException {
    LOGGER.trace("getAddressDTOByAddress()");

    try {
      addressByAddressSelectStatement.setString(1, address);

      return getAddressDTO(addressByAddressSelectStatement);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressDTOByAddress", e);
    }
  }

  /**
   * 
   */
  private @Nullable AddressDTO getAddressDTO(@Nonnull PreparedStatement statement) throws DfxException {
    LOGGER.trace("getAddressDTO()");

    try {
      AddressDTO addressDTO = null;

      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        addressDTO =
            new AddressDTO(
                resultSet.getInt("number"),
                resultSet.getString("address"));

        addressDTO.keepInternalState();
      }

      resultSet.close();

      return addressDTO;
    } catch (Exception e) {
      throw new DfxException("getAddressDTO", e);
    }
  }

  /**
   * 
   */
  public void saveAddress(@Nonnull Map<String, AddressDTO> newAddressMap) throws DfxException {
    LOGGER.trace("saveAddress()");

    try {
      for (AddressDTO addressDTO : newAddressMap.values()) {
        addressInsertStatement.setLong(1, addressDTO.getNumber());
        addressInsertStatement.setString(2, addressDTO.getAddress());

        addressInsertStatement.execute();
      }
    } catch (Exception e) {
      throw new DfxException("saveAddress", e);
    }
  }

  /**
   * 
   */
  public void saveBlock(@Nonnull BlockDTO blockDTO) throws DfxException {
    LOGGER.trace("saveBlock()");

    try {
      Integer blockNumber = blockDTO.getNumber();

      blockInsertStatement.setInt(1, blockNumber);
      blockInsertStatement.setString(2, blockDTO.getHash());
      blockInsertStatement.setLong(3, blockDTO.getTimestamp());

      blockInsertStatement.execute();

      for (TransactionDTO transactionDTO : blockDTO.getTransactionDTOList()) {
        saveTransaction(transactionDTO);
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
  public void saveTransaction(@Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("saveTransaction()");

    try {
      Integer blockNumber = transactionDTO.getBlockNumber();
      Integer transactionNumber = transactionDTO.getNumber();

      transactionInsertStatement.setLong(1, blockNumber);
      transactionInsertStatement.setLong(2, transactionNumber);
      transactionInsertStatement.setString(3, transactionDTO.getTransactionId());
      transactionInsertStatement.setString(4, transactionDTO.getCustomTypeCode());

      transactionInsertStatement.execute();

      // ...
      for (AddressTransactionOutDTO addressTransactionOutDTO : transactionDTO.getAddressTransactionOutDTOList()) {
        saveAddressTransactionOut(addressTransactionOutDTO);
      }

      for (AddressTransactionInDTO addressTransactionInDTO : transactionDTO.getAddressTransactionInDTOList()) {
        saveAddressTransactionIn(addressTransactionInDTO);
      }

      // ...
      saveCustomTransaction(transactionDTO);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("saveTransaction", e);
    }
  }

  /**
   * 
   */
  public void saveAddressTransactionOut(@Nonnull AddressTransactionOutDTO addressTransactionOutDTO) throws DfxException {
    LOGGER.trace("saveAddressTransactionOut()");

    try {
      addressTransactionOutInsertStatement.setInt(1, addressTransactionOutDTO.getBlockNumber());
      addressTransactionOutInsertStatement.setInt(2, addressTransactionOutDTO.getTransactionNumber());
      addressTransactionOutInsertStatement.setInt(3, addressTransactionOutDTO.getVoutNumber());
      addressTransactionOutInsertStatement.setInt(4, addressTransactionOutDTO.getAddressNumber());

      addressTransactionOutInsertStatement.setBigDecimal(5, addressTransactionOutDTO.getVout());
      addressTransactionOutInsertStatement.setString(6, addressTransactionOutDTO.getType());

      addressTransactionOutInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("saveAddressTransactionOut", e);
    }
  }

  /**
   * 
   */
  public void saveAddressTransactionIn(@Nonnull AddressTransactionInDTO addressTransactionInDTO) throws DfxException {
    LOGGER.trace("saveAddressTransactionIn()");

    try {
      addressTransactionInInsertStatement.setInt(1, addressTransactionInDTO.getBlockNumber());
      addressTransactionInInsertStatement.setInt(2, addressTransactionInDTO.getTransactionNumber());
      addressTransactionInInsertStatement.setInt(3, addressTransactionInDTO.getVinNumber());
      addressTransactionInInsertStatement.setInt(4, addressTransactionInDTO.getAddressNumber());

      addressTransactionInInsertStatement.setInt(5, addressTransactionInDTO.getInBlockNumber());
      addressTransactionInInsertStatement.setInt(6, addressTransactionInDTO.getInTransactionNumber());

      addressTransactionInInsertStatement.setBigDecimal(7, addressTransactionInDTO.getVin());

      addressTransactionInInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("saveAddressTransactionIn", e);
    }
  }

  /**
   * 
   */
  public void saveCustomTransaction(@Nonnull TransactionDTO transactionDTO) throws DfxException {
    LOGGER.trace("saveCustomTransaction()");

    for (TransactionCustomAccountToAccountInDTO transactionCustomAccountToAccountInDTO : transactionDTO.getCustomAccountToAccountInDTOList()) {
      saveCustomTransactionAccountToAccountIn(transactionCustomAccountToAccountInDTO);
    }

    for (TransactionCustomAccountToAccountOutDTO transactionCustomAccountToAccountOutDTO : transactionDTO.getCustomAccountToAccountOutDTOList()) {
      saveCustomTransactionAccountToAccountOut(transactionCustomAccountToAccountOutDTO);
    }
  }

  /**
   * 
   */
  private void saveCustomTransactionAccountToAccountIn(@Nonnull TransactionCustomAccountToAccountInDTO transactionCustomAccountToAccountInDTO)
      throws DfxException {
    LOGGER.trace("saveCustomTransactionAccountToAccountIn()");

    try {
      customTransactionAccountToAccountInInsertStatement.setInt(1, transactionCustomAccountToAccountInDTO.getBlockNumber());
      customTransactionAccountToAccountInInsertStatement.setInt(2, transactionCustomAccountToAccountInDTO.getTransactionNumber());
      customTransactionAccountToAccountInInsertStatement.setInt(3, transactionCustomAccountToAccountInDTO.getTypeNumber());
      customTransactionAccountToAccountInInsertStatement.setInt(4, transactionCustomAccountToAccountInDTO.getAddressNumber());
      customTransactionAccountToAccountInInsertStatement.setBigDecimal(5, transactionCustomAccountToAccountInDTO.getAmount());
      customTransactionAccountToAccountInInsertStatement.setInt(6, transactionCustomAccountToAccountInDTO.getTokenNumber());

      customTransactionAccountToAccountInInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("saveCustomTransactionAccountToAccountIn", e);
    }
  }

  /**
   * 
   */
  private void saveCustomTransactionAccountToAccountOut(@Nonnull TransactionCustomAccountToAccountOutDTO transactionCustomAccountToAccountOutDTO)
      throws DfxException {
    LOGGER.trace("saveCustomTransactionAccountToAccountOut()");

    try {
      customTransactionAccountToAccountOutInsertStatement.setInt(1, transactionCustomAccountToAccountOutDTO.getBlockNumber());
      customTransactionAccountToAccountOutInsertStatement.setInt(2, transactionCustomAccountToAccountOutDTO.getTransactionNumber());
      customTransactionAccountToAccountOutInsertStatement.setInt(3, transactionCustomAccountToAccountOutDTO.getTypeNumber());
      customTransactionAccountToAccountOutInsertStatement.setInt(4, transactionCustomAccountToAccountOutDTO.getAddressNumber());
      customTransactionAccountToAccountOutInsertStatement.setBigDecimal(5, transactionCustomAccountToAccountOutDTO.getAmount());
      customTransactionAccountToAccountOutInsertStatement.setInt(6, transactionCustomAccountToAccountOutDTO.getTokenNumber());

      customTransactionAccountToAccountOutInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("saveCustomTransactionAccountToAccountOut", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<MasternodeWhitelistDTO> getMasternodeWhitelistDTOList() throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOList()");

    try {
      List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = new ArrayList<>();

      ResultSet resultSet = masternodeSelectStatement.executeQuery();

      while (resultSet.next()) {
        masternodeWhitelistDTOList.add(createMasternodeWhitelistDTO(resultSet));
      }

      resultSet.close();

      return masternodeWhitelistDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getMasternodeWhitelistDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nullable MasternodeWhitelistDTO getMasternodeWhitelistDTOByOwnerAddress(@Nonnull String ownerAddress) throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOByOwnerAddress()");

    try {
      MasternodeWhitelistDTO masternodeWhitelistDTO = null;

      masternodeWhitelistByOwnerAddressSelectStatement.setString(1, ownerAddress);

      ResultSet resultSet = masternodeWhitelistByOwnerAddressSelectStatement.executeQuery();

      if (resultSet.next()) {
        masternodeWhitelistDTO = createMasternodeWhitelistDTO(resultSet);
      }

      resultSet.close();

      return masternodeWhitelistDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getMasternodeWhitelistDTOByOwnerAddress", e);
    }
  }

  /**
   * 
   */
  private MasternodeWhitelistDTO createMasternodeWhitelistDTO(@Nonnull ResultSet resultSet) throws DfxException {
    LOGGER.trace("createMasternodeWhitelistDTO()");

    try {
      MasternodeWhitelistDTO masternodeWhitelistDTO =
          new MasternodeWhitelistDTO(
              resultSet.getInt("wallet_id"),
              resultSet.getInt("idx"),
              resultSet.getString("owner_address"));

      masternodeWhitelistDTO.setTransactionId(resultSet.getString("txid"));
      masternodeWhitelistDTO.setOperatorAddress(resultSet.getString("operator_address"));
      masternodeWhitelistDTO.setRewardAddress(resultSet.getString("reward_address"));
      masternodeWhitelistDTO.setCreationBlockNumber(resultSet.getInt("creation_block_number"));
      masternodeWhitelistDTO.setResignBlockNumber(resultSet.getInt("resign_block_number"));
      masternodeWhitelistDTO.setState(resultSet.getString("state"));

      masternodeWhitelistDTO.keepInternalState();

      return masternodeWhitelistDTO;
    } catch (Exception e) {
      throw new DfxException("createMasternodeWhitelistDTO", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressTransactionInDTO getAddressTransactionInDTO(
      int blockNumber,
      int transactionNumber) throws DfxException {
    LOGGER.trace("getAddressTransactionInDTO()");

    try {
      AddressTransactionInDTO addressTransactionInDTO = null;

      addressTransactionInByBlockAndTransactionSelectStatement.setInt(1, blockNumber);
      addressTransactionInByBlockAndTransactionSelectStatement.setInt(2, transactionNumber);

      ResultSet resultSet = addressTransactionInByBlockAndTransactionSelectStatement.executeQuery();

      if (resultSet.next()) {
        addressTransactionInDTO =
            new AddressTransactionInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vin_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("in_block_number"),
                resultSet.getInt("in_transaction_number"));
        addressTransactionInDTO.setVin(resultSet.getBigDecimal("vin"));

        addressTransactionInDTO.keepInternalState();
      }

      resultSet.close();

      return addressTransactionInDTO;
    } catch (Exception e) {
      throw new DfxException("getAddressTransactionInDTO", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressTransactionOutDTO getAddressTransactionOutDTO(
      int blockNumber,
      int transactionNumber) throws DfxException {
    LOGGER.trace("getCustomTransactionAccountToAccountOutDTO()");

    try {
      AddressTransactionOutDTO addressTransactionOutDTO = null;

      addressTransactionOutByBlockAndTransactionSelectStatement.setInt(1, blockNumber);
      addressTransactionOutByBlockAndTransactionSelectStatement.setInt(2, transactionNumber);

      ResultSet resultSet = addressTransactionOutByBlockAndTransactionSelectStatement.executeQuery();

      if (resultSet.next()) {
        addressTransactionOutDTO =
            new AddressTransactionOutDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vout_number"),
                resultSet.getInt("address_number"));
        addressTransactionOutDTO.setVout(resultSet.getBigDecimal("vout"));

        addressTransactionOutDTO.keepInternalState();
      }

      resultSet.close();

      return addressTransactionOutDTO;
    } catch (Exception e) {
      throw new DfxException("getCustomTransactionAccountToAccountOutDTO", e);
    }
  }
}
