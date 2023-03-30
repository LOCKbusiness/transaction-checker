package ch.dfx.manager.checker.transaction;

import java.sql.Connection;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.transactionserver.data.VaultWhitelistDTO;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class VaultWhitelistChecker {
  private static final Logger LOGGER = LogManager.getLogger(VaultWhitelistChecker.class);

  // ...
  private final H2DBManager databaseManager;
  private final DatabaseBlockHelper databaseBlockHelper;

  /**
   * 
   */
  public VaultWhitelistChecker(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {

    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
  }

  /**
   * 
   */
  public boolean checkVaultWhitelist(@Nonnull String vaultId) {
    LOGGER.trace("checkVaultWhitelist()");

    return checkVaultWhitelist(Arrays.asList(vaultId));
  }

  /**
   * 
   */
  public boolean checkVaultWhitelist(@Nonnull List<String> vaultIdList) {
    LOGGER.trace("checkVaultWhitelist()");

    // ...
    boolean isValid;

    // ...
    BitSet bitSet = new BitSet(vaultIdList.size());
    bitSet.clear();

    // ...
    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);

      // ...
      for (int i = 0; i < vaultIdList.size(); i++) {
        String vaultId = vaultIdList.get(i);

        VaultWhitelistDTO vaultWhitelistDTO =
            databaseBlockHelper.getVaultWhitelistDTOByVaultId(vaultId);

        if (null != vaultWhitelistDTO
            && vaultId.equals(vaultWhitelistDTO.getVaultId())) {
          bitSet.set(i);
        }
      }

      isValid = bitSet.cardinality() == vaultIdList.size();

      // ...
      databaseBlockHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkVaultWhitelist", e);
      isValid = false;
    } finally {
      databaseManager.closeConnection(connection);
    }

    return isValid;
  }
}
