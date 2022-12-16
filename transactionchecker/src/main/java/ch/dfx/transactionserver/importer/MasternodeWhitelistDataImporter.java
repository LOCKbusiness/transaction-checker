package ch.dfx.transactionserver.importer;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.importer.data.DataImporterMasternodeOwnerData;
import ch.dfx.transactionserver.importer.data.DataImporterMasternodeOwnerDataList;

/**
 * 
 */
public class MasternodeWhitelistDataImporter {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeWhitelistDataImporter.class);

  private final NetworkEnum network;

  private final Gson gson;

  // ...
  private final H2DBManager databaseManager;

  // ...
  private PreparedStatement masternodeWhitelistInsertStatement = null;

  /**
   * 
   */
  public MasternodeWhitelistDataImporter(@Nonnull NetworkEnum network) {
    this.network = network;

    this.databaseManager = new H2DBManagerImpl();

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.trace("execute()");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      openStatements(connection);

      Path path = getPath();
      DataImporterMasternodeOwnerDataList dataImporterMasternodeOwnerDataList =
          gson.fromJson(Files.readString(path), DataImporterMasternodeOwnerDataList.class);

      for (DataImporterMasternodeOwnerData dataImporterMasternodeOwnerData : dataImporterMasternodeOwnerDataList) {
        masternodeWhitelistInsertStatement.setInt(1, 1);
        masternodeWhitelistInsertStatement.setInt(2, dataImporterMasternodeOwnerData.getIndex());
        masternodeWhitelistInsertStatement.setString(3, dataImporterMasternodeOwnerData.getAddress());
        masternodeWhitelistInsertStatement.execute();
      }

      closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("execute", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private Path getPath() {
    Path path;

    if (NetworkEnum.MAINNET == network) {
      path = Paths.get("data", "json", "masternode", "owner-prd.json");
    } else if (NetworkEnum.STAGNET == network) {
      path = Paths.get("data", "json", "masternode", "owner-prd.json");
    } else {
      path = Paths.get("data", "json", "masternode", "owner-dev.json");
    }

    return path;
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String masternodeWhitelistInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".masternode_whitelist (wallet_id, idx, owner_address) VALUES (?, ?, ?)";
      masternodeWhitelistInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, masternodeWhitelistInsertSql));
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
      masternodeWhitelistInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }
}
