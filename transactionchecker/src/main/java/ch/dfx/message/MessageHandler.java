package ch.dfx.message;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class MessageHandler {
  private static final Logger LOGGER = LogManager.getLogger(MessageHandler.class);

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public MessageHandler(@Nonnull DefiDataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  /**
   * 
   */
  public String signMessage(@Nonnull String message) throws DfxException {
    LOGGER.trace("signMessage() ...");

    String wallet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
    String password = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_PASSWORD);
    String signAddress = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_SIGN_ADDRESS);

    dataProvider.walletPassphrase(wallet, password, 10);
    String signedMessage = dataProvider.signMessage(wallet, signAddress, message);
    dataProvider.walletLock(wallet);

    return signedMessage;
  }

  /**
   * 
   */
  public Boolean verifyMessage(
      @Nonnull String verifyAddress,
      @Nonnull String signature,
      @Nonnull String message) throws DfxException {
    LOGGER.trace("verifyMessage() ...");
    return dataProvider.verifyMessage(verifyAddress, signature, message);
  }
}
