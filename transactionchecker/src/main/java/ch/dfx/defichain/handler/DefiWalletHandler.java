package ch.dfx.defichain.handler;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.wallet.DefiLoadWalletData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class DefiWalletHandler {
  private static final Logger LOGGER = LogManager.getLogger(DefiWalletHandler.class);

  private final NetworkEnum network;
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public DefiWalletHandler(
      @Nonnull NetworkEnum network,
      @Nonnull DefiDataProvider dataProvider) {
    this.network = network;
    this.dataProvider = dataProvider;
  }

  /**
   * 
   */
  public void loadWallet(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("loadWallet()");

    if (NetworkEnum.STAGNET != network) {
      List<String> walletList = dataProvider.listWallets();

      if (!walletList.contains(wallet)) {
        DefiLoadWalletData loadWalletData = dataProvider.loadWallet(wallet);
        String warning = loadWalletData.getWarning();

        if (!StringUtils.isEmpty(warning)) {
          LOGGER.warn("loadWallet: " + warning);
        }
      }
    }
  }

  /**
   * 
   */
  public void unloadWallet(@Nonnull String wallet) throws DfxException {
    LOGGER.trace("unloadWallet()");

    if (NetworkEnum.STAGNET != network) {
      List<String> walletList = dataProvider.listWallets();

      if (walletList.contains(wallet)) {
        dataProvider.unloadWallet(wallet);
      }
    }
  }
}
