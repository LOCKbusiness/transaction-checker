package ch.dfx.transactionserver.importer;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;

/**
 * 
 */
public class MasternodeWhitelistDataImporter extends WhitelistDataImporter {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeWhitelistDataImporter.class);

  // ...
  private static final int WALLET_ID = 1;

  /**
   * 
   */
  public MasternodeWhitelistDataImporter(@Nonnull NetworkEnum network) {
    super(network);
  }

  /**
   * 
   */
  @Override
  public Path getPath() {
    LOGGER.trace("getPath()");

    Path path;

    if (NetworkEnum.MAINNET == network) {
      path = Paths.get("data", "json", "masternode", "owner-prd.json");
    } else if (NetworkEnum.STAGNET == network) {
      path = Paths.get("data", "json", "masternode", "owner-stg.json");
    } else {
      path = Paths.get("data", "json", "masternode", "owner-dev.json");
    }

    return path;
  }

  @Override
  public int getWalletId() {
    return WALLET_ID;
  }
}
