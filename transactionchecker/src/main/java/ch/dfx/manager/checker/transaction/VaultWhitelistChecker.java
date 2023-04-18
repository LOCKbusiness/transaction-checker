package ch.dfx.manager.checker.transaction;

import java.sql.Connection;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  public boolean checkVaultIdWhitelist(@Nonnull String vaultId) {
    LOGGER.trace("checkVaultIdWhitelist()");

    return checkVaultIdWhitelist(Arrays.asList(vaultId));
  }

  /**
   * 
   */
  public boolean checkVaultIdWhitelist(@Nonnull List<String> vaultIdList) {
    LOGGER.trace("checkVaultIdWhitelist()");

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
      Set<String> vaultIdCheckSet = new HashSet<>();
      List<VaultWhitelistDTO> vaultWhitelistDTOList = databaseBlockHelper.getVaultWhitelistDTOList();
      vaultWhitelistDTOList.forEach(dto -> vaultIdCheckSet.add(dto.getVaultId()));

      // ...
      for (int i = 0; i < vaultIdList.size(); i++) {
        String vaultId = vaultIdList.get(i);

        if (vaultIdCheckSet.contains(vaultId)) {
          bitSet.set(i);
        }
      }

      isValid = bitSet.cardinality() == vaultIdList.size();

      // ...
      databaseBlockHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkVaultIdWhitelist", e);
      isValid = false;
    } finally {
      databaseManager.closeConnection(connection);
    }

    return isValid;
  }

  /**
   * 
   */
  public boolean checkVaultAddressWhitelist(@Nonnull String vaultAddress) {
    LOGGER.trace("checkVaultAddressWhitelist()");

    return checkVaultAddressWhitelist(Arrays.asList(vaultAddress));
  }

  /**
   * 
   */
  public boolean checkVaultAddressWhitelist(@Nonnull List<String> vaultAddressList) {
    LOGGER.trace("checkVaultAddressWhitelist()");

    // ...
    boolean isValid;

    // ...
    BitSet bitSet = new BitSet(vaultAddressList.size());
    bitSet.clear();

    // ...
    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);

      // ...
      Set<String> vaultAddressCheckSet = new HashSet<>();
      List<VaultWhitelistDTO> vaultWhitelistDTOList = databaseBlockHelper.getVaultWhitelistDTOList();
      vaultWhitelistDTOList.forEach(dto -> vaultAddressCheckSet.add(dto.getAddress()));

      // ...
      for (int i = 0; i < vaultAddressList.size(); i++) {
        String vaultAddress = vaultAddressList.get(i);

        if (vaultAddressCheckSet.contains(vaultAddress)) {
          bitSet.set(i);
        }
      }

      isValid = bitSet.cardinality() == vaultAddressList.size();

      // ...
      databaseBlockHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkVaultAddressWhitelist", e);
      isValid = false;
    } finally {
      databaseManager.closeConnection(connection);
    }

    return isValid;
  }
}
