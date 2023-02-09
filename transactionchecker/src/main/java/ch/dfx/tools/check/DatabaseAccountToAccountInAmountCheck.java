package ch.dfx.tools.check;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.builder.DatabaseCustomTransactionBuilder;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.BlockTransactionDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountInDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountOutDTO;
import ch.dfx.transactionserver.data.TransactionDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.handler.DatabaseAddressHandler;

/**
 * 
 */
public class DatabaseAccountToAccountInAmountCheck {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseAccountToAccountInAmountCheck.class);

  // ...
  private PreparedStatement accountToAccountOutSelectStatement = null;
  private PreparedStatement accountToAccountInSelectStatement = null;

  private PreparedStatement transactionCustomAccountToAccountInUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBlockHelper databaseBlockHelper;

  private final DatabaseAddressHandler databaseAddressHandler;
  private final DatabaseCustomTransactionBuilder customTransactionBuilder;

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public DatabaseAccountToAccountInAmountCheck(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper) {
    this.network = network;
    this.databaseBlockHelper = databaseBlockHelper;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.databaseAddressHandler = new DatabaseAddressHandler(network);
    this.customTransactionBuilder = new DatabaseCustomTransactionBuilder(network, databaseBlockHelper, databaseAddressHandler);
  }

  /**
   * 
   */
  public void check(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("check()");

    try {
      openStatements(connection);

      // ...
      customTransactionBuilder.fillCustomTypeCodeToNumberMap(connection);
      databaseAddressHandler.setup(connection);

      // ...
      doCheck(connection);

      closeStatements();
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("check", e);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String accountToAccountOutSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".ACCOUNT_TO_ACCOUNT_OUT"
              + " WHERE type_number = 5"
              + " AND address_number = 2829489"
              + " ORDER BY block_number, transaction_number"
              + " LIMIT ? OFFSET ?";
      accountToAccountOutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, accountToAccountOutSelectSql));

      String accountToAccountInSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".ACCOUNT_TO_ACCOUNT_IN"
              + " WHERE block_number=? AND transaction_number=?";
      accountToAccountInSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, accountToAccountInSelectSql));

      // ...
      String transactionCustomAccountToAccountInUpdateSql =
          "UPDATE " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in"
              + " SET amount=?"
              + " WHERE block_number=? AND transaction_number=?";
      transactionCustomAccountToAccountInUpdateStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, transactionCustomAccountToAccountInUpdateSql));
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
      accountToAccountOutSelectStatement.close();
      accountToAccountInSelectStatement.close();

      transactionCustomAccountToAccountInUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void doCheck(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("doCheck()");

    try {
      int limit = 1000;
      int offset = 0;

      for (int i = 0; i < 100000; i++) {
        accountToAccountOutSelectStatement.setInt(1, limit);
        accountToAccountOutSelectStatement.setInt(2, offset);
        offset += limit;

        TransactionCustomAccountToAccountOutDTO transactionCustomAccountToAccountOutDTO = null;

        ResultSet resultSet = accountToAccountOutSelectStatement.executeQuery();

        while (resultSet.next()) {
          transactionCustomAccountToAccountOutDTO =
              new TransactionCustomAccountToAccountOutDTO(
                  resultSet.getInt("block_number"),
                  resultSet.getInt("transaction_number"),
                  resultSet.getInt("type_number"),
                  resultSet.getInt("address_number"),
                  resultSet.getInt("token_number"));
          transactionCustomAccountToAccountOutDTO.setAmount(resultSet.getBigDecimal("amount"));
          transactionCustomAccountToAccountOutDTO.keepInternalState();

          int blockNumber = transactionCustomAccountToAccountOutDTO.getBlockNumber();
          int transactionNumber = transactionCustomAccountToAccountOutDTO.getTransactionNumber();

          LOGGER.debug("Block: " + blockNumber + " / " + transactionNumber);

          TransactionCustomAccountToAccountInDTO transactionCustomAccountToAccountInDTO =
              getTransactionCustomAccountToAccountInDTO(blockNumber, transactionNumber);

          if (null != transactionCustomAccountToAccountInDTO) {
            checkTransactionCustomAccountToAccountIn(connection, transactionCustomAccountToAccountInDTO);
          }
        }

        resultSet.close();

        if (null == transactionCustomAccountToAccountOutDTO) {
          break;
        }
      }

    } catch (Exception e) {
      throw new DfxException("doCheck", e);
    }
  }

  /**
   * 
   */
  private void checkTransactionCustomAccountToAccountIn(
      @Nonnull Connection connection,
      @Nonnull TransactionCustomAccountToAccountInDTO databaseTransactionCustomAccountToAccountInDTO) throws DfxException {
    LOGGER.trace("checkTransactionCustomAccountToAccountIn()");

    int blockNumber = databaseTransactionCustomAccountToAccountInDTO.getBlockNumber();
    int transactionNumber = databaseTransactionCustomAccountToAccountInDTO.getTransactionNumber();

    TransactionDTO onChainCustomTransactionDTO = createOnChainCustomTransaction(blockNumber, transactionNumber);

    List<TransactionCustomAccountToAccountInDTO> onChainCustomAccountToAccountInDTOList =
        onChainCustomTransactionDTO.getCustomAccountToAccountInDTOList();

    if (1 == onChainCustomAccountToAccountInDTOList.size()) {
      TransactionCustomAccountToAccountInDTO onChainTransactionCustomAccountToAccountInDTO =
          onChainCustomAccountToAccountInDTOList.get(0);
      BigDecimal onChainAmount = onChainTransactionCustomAccountToAccountInDTO.getAmount();
      BigDecimal databaseAmount = databaseTransactionCustomAccountToAccountInDTO.getAmount();

      if (0 != onChainAmount.compareTo(databaseAmount)) {
        updateTransactionCustomAccountToAccountIn(connection, blockNumber, transactionNumber, onChainAmount);
      }
    }
  }

  /**
   * 
   */
  private @Nullable TransactionCustomAccountToAccountInDTO getTransactionCustomAccountToAccountInDTO(int blockNumber, int transactionNumber)
      throws DfxException {
    LOGGER.trace("getTransactionCustomAccountToAccountInDTO()");

    try {
      TransactionCustomAccountToAccountInDTO transactionCustomAccountToAccountInDTO = null;
      accountToAccountInSelectStatement.setInt(1, blockNumber);
      accountToAccountInSelectStatement.setInt(2, transactionNumber);

      ResultSet resultSet = accountToAccountInSelectStatement.executeQuery();

      if (resultSet.next()) {
        transactionCustomAccountToAccountInDTO =
            new TransactionCustomAccountToAccountInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("type_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("token_number"));
        transactionCustomAccountToAccountInDTO.setAmount(resultSet.getBigDecimal("amount"));
        transactionCustomAccountToAccountInDTO.keepInternalState();
      }

      resultSet.close();

      return transactionCustomAccountToAccountInDTO;
    } catch (Exception e) {
      throw new DfxException("getTransactionCustomAccountToAccountInDTO", e);
    }
  }

  /**
   * 
   */
  private TransactionDTO createOnChainCustomTransaction(int blockNumber, int transactionNumber) throws DfxException {
    LOGGER.trace("createOnChainCustomTransaction()");

    TransactionDTO customTransactionDTO = null;

    BlockDTO blockDTO = databaseBlockHelper.getBlockDTOByNumber(blockNumber);
    TransactionDTO transactionDTO = databaseBlockHelper.getTransactionDTOByBlockNumberAndNumber(blockNumber, transactionNumber);

    if (null != blockDTO
        && null != transactionDTO) {
      BlockTransactionDTO blockTransactionDTO =
          new BlockTransactionDTO(blockNumber, blockDTO.getHash(),
              transactionNumber, transactionDTO.getTransactionId());

      customTransactionDTO = createOnChainCustomTransaction(blockTransactionDTO);
    }

    return customTransactionDTO;
  }

  /**
   * 
   */
  private TransactionDTO createOnChainCustomTransaction(@Nonnull BlockTransactionDTO blockTransactionDTO) throws DfxException {
    LOGGER.trace("createOnChainCustomTransaction()");

    Integer blockNumber = blockTransactionDTO.getBlockNumber();
    String blockHash = blockTransactionDTO.getBlockHash();
    Integer transactionNumber = blockTransactionDTO.getTransactionNumber();
    String transactionId = blockTransactionDTO.getTransactionId();

    DefiTransactionData transactionData = dataProvider.getTransaction(transactionId, blockHash);

    TransactionDTO transactionDTO = new TransactionDTO(blockNumber, transactionNumber, transactionId);

    customTransactionBuilder.fillCustomTransactionInfo(transactionData, transactionDTO);

    return transactionDTO;
  }

  /**
   * 
   */
  private void updateTransactionCustomAccountToAccountIn(
      @Nonnull Connection connection,
      int blockNumber,
      int transactionNumber,
      @Nonnull BigDecimal amount) throws DfxException {
    LOGGER.trace("updateTransactionCustomAccountToAccountIn()");

    try {
      transactionCustomAccountToAccountInUpdateStatement.setBigDecimal(1, amount);
      transactionCustomAccountToAccountInUpdateStatement.setInt(2, blockNumber);
      transactionCustomAccountToAccountInUpdateStatement.setInt(3, transactionNumber);
      transactionCustomAccountToAccountInUpdateStatement.execute();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("updateTransactionCustomAccountToAccountIn", e);
    }
  }
}
