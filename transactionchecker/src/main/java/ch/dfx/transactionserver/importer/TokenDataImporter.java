package ch.dfx.transactionserver.importer;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import ch.dfx.transactionserver.importer.data.DataImporterTokenData;
import ch.dfx.transactionserver.importer.data.DataImporterTokenDataMap;

/**
 * 
 */
public class TokenDataImporter {
  private static final Logger LOGGER = LogManager.getLogger(TokenDataImporter.class);

  private final NetworkEnum network;

  private final Gson gson;

  // ...
  private final H2DBManager databaseManager;

  // ...
  private PreparedStatement tokenSelectStatement = null;
  private PreparedStatement tokenInsertStatement = null;

  /**
   * 
   */
  public TokenDataImporter(@Nonnull NetworkEnum network) {
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

      Map<Integer, String> tokenMap = getAllTokens();

      Path path = getPath();

      DataImporterTokenDataMap dataImporterTokenDataMap =
          gson.fromJson(Files.readString(path), DataImporterTokenDataMap.class);

      for (Entry<String, DataImporterTokenData> dataImporterTokenDataMapEntry : dataImporterTokenDataMap.entrySet()) {
        DataImporterTokenData dataImporterTokenData = dataImporterTokenDataMapEntry.getValue();

        if (dataImporterTokenData.isDAT()
            || dataImporterTokenData.isLPS()) {
          int tokenNumber = Integer.parseInt(dataImporterTokenDataMapEntry.getKey());
          String tokenName = dataImporterTokenData.getSymbol();

          if (!tokenMap.containsKey(tokenNumber)) {
            tokenInsertStatement.setInt(1, tokenNumber);
            tokenInsertStatement.setString(2, tokenName);
            tokenInsertStatement.execute();
          }
        }
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
      path = Paths.get("data", "json", "token", "defichain-token-main.json");
    } else if (NetworkEnum.STAGNET == network) {
      path = Paths.get("data", "json", "token", "defichain-token-main.json");
    } else {
      path = Paths.get("data", "json", "token", "defichain-token-test.json");
    }

    return path;
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String tokenSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".token";
      tokenSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, tokenSelectSql));

      String tokenInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".token (number, name) VALUES (?, ?)";
      tokenInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, tokenInsertSql));
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
      tokenSelectStatement.close();
      tokenInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private Map<Integer, String> getAllTokens() throws DfxException {
    LOGGER.trace("getAllTokens()");

    try {
      Map<Integer, String> tokenMap = new HashMap<>();

      ResultSet resultSet = tokenSelectStatement.executeQuery();

      while (resultSet.next()) {
        tokenMap.put(resultSet.getInt("number"), resultSet.getString("name"));
      }

      resultSet.close();

      return tokenMap;
    } catch (Exception e) {
      throw new DfxException("getAllTokens", e);
    }
  }
}
