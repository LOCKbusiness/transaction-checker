package ch.dfx.transactionserver.importer;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.importer.data.DataImporterMasternodeOwnerData;
import ch.dfx.transactionserver.importer.data.DataImporterMasternodeOwnerDataList;

/**
 * 
 */
public abstract class WhitelistDataImporter {
  private static final Logger LOGGER = LogManager.getLogger(WhitelistDataImporter.class);

  // ...
  protected final NetworkEnum network;

  private final Gson gson;

  // ...
  private final H2DBManager databaseManager;
  private final DatabaseBalanceHelper databaseBalanceHelper;

  // ...
  private PreparedStatement masternodeWhitelistInsertStatement = null;

  /**
   * 
   */
  public WhitelistDataImporter(@Nonnull NetworkEnum network) {
    this.network = network;

    this.databaseManager = new H2DBManagerImpl();
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  public abstract Path getPath();

  public abstract int getWalletId();

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.trace("execute()");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBalanceHelper.openStatements(connection);
      openStatements(connection);

      Set<String> ownerAddressSet = getOwnerAddressSet();

      Path path = getPath();
      DataImporterMasternodeOwnerDataList dataImporterMasternodeOwnerDataList =
          gson.fromJson(Files.readString(path), DataImporterMasternodeOwnerDataList.class);

      for (DataImporterMasternodeOwnerData dataImporterMasternodeOwnerData : dataImporterMasternodeOwnerDataList) {
        String ownerAddress = dataImporterMasternodeOwnerData.getAddress();

        if (!ownerAddressSet.contains(ownerAddress)) {
          masternodeWhitelistInsertStatement.setInt(1, getWalletId());
          masternodeWhitelistInsertStatement.setInt(2, dataImporterMasternodeOwnerData.getIndex());
          masternodeWhitelistInsertStatement.setString(3, ownerAddress);
          masternodeWhitelistInsertStatement.execute();
        }
      }

      closeStatements();
      databaseBalanceHelper.closeStatements();

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
  private Set<String> getOwnerAddressSet() throws DfxException {
    LOGGER.trace("getOwnerAddressSet()");

    Set<String> ownerAddressSet = new HashSet<>();

    List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = databaseBalanceHelper.getMasternodeWhitelistDTOList();
    masternodeWhitelistDTOList.forEach(dto -> ownerAddressSet.add(dto.getOwnerAddress()));

    return ownerAddressSet;
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
