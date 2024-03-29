package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.masternode.DefiMasternodeData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class MasternodeBuilder {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeBuilder.class);

  private PreparedStatement masternodeSelectStatement = null;
  private PreparedStatement masternodeTransactionSelectStatement = null;

  private PreparedStatement masternodeUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBlockHelper databaseBlockHelper;

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public MasternodeBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper) {
    this.network = network;

    this.databaseBlockHelper = databaseBlockHelper;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void build(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = databaseBlockHelper.getMasternodeWhitelistDTOList();

      fillMasternodeWhitelistDTO(masternodeWhitelistDTOList);
      updateMasternodeWhitelistDTO(connection, masternodeWhitelistDTOList);

      closeStatements();
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("build", e);
    } finally {
      LOGGER.debug("[MasternodeBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String masternodeSelectSql = "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".masternode_whitelist";
      masternodeSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, masternodeSelectSql));

      String masternodeUpdateSql =
          "UPDATE " + TOKEN_NETWORK_SCHEMA + ".masternode_whitelist"
              + " SET txid=?"
              + ", operator_address=?"
              + ", reward_address=?"
              + ", creation_block_number=?"
              + ", resign_block_number=?"
              + ", state=?"
              + " WHERE wallet_id=? AND owner_address=?";
      masternodeUpdateStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, masternodeUpdateSql));

      // ...
      String masternodeTransactionSelectSql =
          "SELECT"
              + " t.txid"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " at_in.block_number = at_out.block_number"
              + " AND at_in.transaction_number = at_out.transaction_number"
              + " AND at_in.address_number = at_out.address_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".transaction t ON"
              + " at_in.block_number = t.block_number"
              + " AND at_in.transaction_number = t.number"
              + " WHERE at_in.address_number=?";
      masternodeTransactionSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, masternodeTransactionSelectSql));
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
      masternodeSelectStatement.close();
      masternodeTransactionSelectStatement.close();

      masternodeUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void fillMasternodeWhitelistDTO(@Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList) throws DfxException {
    LOGGER.trace("fillMasternodeWhitelistDTO()");

    for (MasternodeWhitelistDTO masternodeWhitelistDTO : masternodeWhitelistDTOList) {
      String ownerAddress = masternodeWhitelistDTO.getOwnerAddress();

      if (null == masternodeWhitelistDTO.getTransactionId()) {
        AddressDTO addressDTO = databaseBlockHelper.getAddressDTOByAddress(ownerAddress);

        if (null != addressDTO) {
          LOGGER.trace("Fill New Masternode " + masternodeWhitelistDTO.getIdx() + ": " + ownerAddress);
          fillNewMasternodeWhitelistDTO(masternodeWhitelistDTO, addressDTO);
        }
      } else {
        LOGGER.trace("Fill Masternode " + masternodeWhitelistDTO.getIdx() + ": " + ownerAddress);
        fillMasternodeWhitelistDTO(masternodeWhitelistDTO);
      }
    }
  }

  /**
   * 
   */
  private void fillNewMasternodeWhitelistDTO(
      @Nonnull MasternodeWhitelistDTO masternodeWhitelistDTO,
      @Nonnull AddressDTO ownerAddressDTO) throws DfxException {
    LOGGER.trace("fillNewMasternodeWhitelistDTO()");

    try {
      masternodeTransactionSelectStatement.setInt(1, ownerAddressDTO.getNumber());

      ResultSet resultSet = masternodeTransactionSelectStatement.executeQuery();

      if (resultSet.next()) {
        String transactionId = resultSet.getString("txid");

        Map<String, DefiMasternodeData> masternodeMap = dataProvider.getMasternode("", transactionId);
        DefiMasternodeData masternodeData = masternodeMap.get(transactionId);

        if (null != masternodeData) {
          masternodeWhitelistDTO.setTransactionId(transactionId);
          masternodeWhitelistDTO.setOperatorAddress(masternodeData.getOperatorAuthAddress());
          masternodeWhitelistDTO.setRewardAddress(masternodeData.getRewardAddress());
          masternodeWhitelistDTO.setCreationBlockNumber(masternodeData.getCreationHeight().intValue());
          masternodeWhitelistDTO.setResignBlockNumber(masternodeData.getResignHeight().intValue());
          masternodeWhitelistDTO.setState(masternodeData.getState());
        }
      }

      resultSet.close();
    } catch (Exception e) {
      throw new DfxException("fillNewMasternodeWhitelistDTO", e);
    }
  }

  /**
   * 
   */
  private void fillMasternodeWhitelistDTO(@Nonnull MasternodeWhitelistDTO masternodeWhitelistDTO) throws DfxException {
    LOGGER.trace("fillMasternodeWhitelistDTO()");

    try {
      String transactionId = masternodeWhitelistDTO.getTransactionId();

      Map<String, DefiMasternodeData> masternodeMap = dataProvider.getMasternode("", transactionId);
      DefiMasternodeData masternodeData = masternodeMap.get(transactionId);

      if (null != masternodeData) {
        masternodeWhitelistDTO.setOperatorAddress(masternodeData.getOperatorAuthAddress());
        masternodeWhitelistDTO.setRewardAddress(masternodeData.getRewardAddress());
        masternodeWhitelistDTO.setCreationBlockNumber(masternodeData.getCreationHeight().intValue());
        masternodeWhitelistDTO.setResignBlockNumber(masternodeData.getResignHeight().intValue());
        masternodeWhitelistDTO.setState(masternodeData.getState());
      }
    } catch (Exception e) {
      throw new DfxException("fillMasternodeWhitelistDTO", e);
    }
  }

  /**
   * 
   */
  private void updateMasternodeWhitelistDTO(
      @Nonnull Connection connection,
      @Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList) throws DfxException {
    LOGGER.trace("updateMasternodeWhitelistDTO()");

    for (MasternodeWhitelistDTO masternodeWhitelistDTO : masternodeWhitelistDTOList) {
      if (masternodeWhitelistDTO.isInternalStateChanged()) {
        updateMasternodeWhitelistDTO(connection, masternodeWhitelistDTO);
      }
    }
  }

  /**
   * 
   */
  private void updateMasternodeWhitelistDTO(
      @Nonnull Connection connection,
      @Nonnull MasternodeWhitelistDTO masternodeWhitelistDTO) throws DfxException {
    LOGGER.trace("updateMasternodeWhitelistDTO()");

    try {
      String ownerAddress = masternodeWhitelistDTO.getOwnerAddress();

      LOGGER.debug("[UPDATE] Owner Address: " + ownerAddress);

      masternodeUpdateStatement.setString(1, masternodeWhitelistDTO.getTransactionId());
      masternodeUpdateStatement.setString(2, masternodeWhitelistDTO.getOperatorAddress());
      masternodeUpdateStatement.setString(3, masternodeWhitelistDTO.getRewardAddress());
      masternodeUpdateStatement.setInt(4, masternodeWhitelistDTO.getCreationBlockNumber());
      masternodeUpdateStatement.setInt(5, masternodeWhitelistDTO.getResignBlockNumber());
      masternodeUpdateStatement.setString(6, masternodeWhitelistDTO.getState());

      masternodeUpdateStatement.setInt(7, masternodeWhitelistDTO.getWalletId());
      masternodeUpdateStatement.setString(8, ownerAddress);

      masternodeUpdateStatement.execute();
      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("updateMasternodeWhitelistDTO", e);
    }
  }
}
