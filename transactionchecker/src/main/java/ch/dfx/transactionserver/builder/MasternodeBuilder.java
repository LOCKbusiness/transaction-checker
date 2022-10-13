package ch.dfx.transactionserver.builder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.OpenTransactionMasternodeDTO;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class MasternodeBuilder {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeBuilder.class);

  private PreparedStatement masternodeSetupSelectStatement = null;
  private PreparedStatement masternodeSetupInsertStatement = null;
  private PreparedStatement masternodeSetupDeleteStatement = null;

  private PreparedStatement masternodeSelectStatement = null;
  private PreparedStatement masternodeInsertStatement = null;
  private PreparedStatement masternodeUpdateStatement = null;

  /**
   * 
   */
  public MasternodeBuilder() {
  }

  /**
   * 
   */
  public void build(@Nonnull List<OpenTransactionMasternodeDTO> openTransactionMasternodeDTOList) throws DfxException {
    LOGGER.trace("build() ...");

    Connection connection = null;

    try {
      connection = H2DBManager.getInstance().openConnection();

      openStatements(connection);

      insertMasternodeSetup(openTransactionMasternodeDTOList);

      closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      H2DBManager.getInstance().closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      // Masternode Setup ...
      String masternodeSetupSelectSql = "SELECT * FROM public.masternode_setup";
      masternodeSetupSelectStatement = connection.prepareStatement(masternodeSetupSelectSql);

      String masternodeSetupInsertSql =
          "INSERT INTO public.masternode_setup"
              + " (txid, owner_address, operator_address, reward_address)"
              + " VALUES (?, ?, ?, ?)";
      masternodeSetupInsertStatement = connection.prepareStatement(masternodeSetupInsertSql);

      String masternodeSetupDeleteSql = "DELETE FROM public.masternode_setup WHERE txid=?";
      masternodeSetupDeleteStatement = connection.prepareStatement(masternodeSetupDeleteSql);

      // Masternode ...
      String masternodeSelectSql = "SELECT * FROM public.masternode";
      masternodeSelectStatement = connection.prepareStatement(masternodeSelectSql);

      String masternodeInsertSql =
          "INSERT INTO public.masternode"
              + " (transaction_number, owner_address_number, operator_address_number, reward_address_number, creation_block_number, resign_block_number, state)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?)";
      masternodeInsertStatement = connection.prepareStatement(masternodeInsertSql);

      String masternodeUpdateSql =
          "UPDATE public.masternode"
              + " SET reward_address_number=?"
              + ", resign_block_number=?"
              + ", state=?"
              + " WHERE transaction_number=?";
      masternodeUpdateStatement = connection.prepareStatement(masternodeUpdateSql);

    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements() ...");

    try {
      masternodeSetupSelectStatement.close();
      masternodeSetupInsertStatement.close();
      masternodeSetupDeleteStatement.close();

      masternodeSelectStatement.close();
      masternodeInsertStatement.close();
      masternodeUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void insertMasternodeSetup(@Nonnull List<OpenTransactionMasternodeDTO> openTransactionMasternodeDTOList) throws DfxException {
    try {
      for (OpenTransactionMasternodeDTO openTransactionMasternodeDTO : openTransactionMasternodeDTOList) {
        String transactionId = openTransactionMasternodeDTO.getTransactionId();
        String ownerAddress = openTransactionMasternodeDTO.getOwnerAddress();
        String operatorAddress = openTransactionMasternodeDTO.getOperatorAddress();

        LOGGER.debug(
            "[INSERT] Transaction ID / Owner Address / Operator Address: "
                + transactionId + " / " + ownerAddress + " / " + operatorAddress);

        masternodeSetupInsertStatement.setString(1, transactionId);
        masternodeSetupInsertStatement.setString(2, ownerAddress);
        masternodeSetupInsertStatement.setString(3, operatorAddress);
        masternodeSetupInsertStatement.setString(4, openTransactionMasternodeDTO.getRewardAddress());
        masternodeSetupInsertStatement.execute();
      }
    } catch (Exception e) {
      throw new DfxException("insertMasternodeSetup", e);
    }
  }
}
