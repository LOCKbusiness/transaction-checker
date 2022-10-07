package ch.dfx.balance;

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
public class MessageChecker {
  private static final Logger LOGGER = LogManager.getLogger(MessageChecker.class);

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public MessageChecker(@Nonnull DefiDataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  /**
   * 
   */
  public String signMessage(@Nonnull String message) throws DfxException {
    LOGGER.trace("signMessage() ...");

    String wallet = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_NAME);
    String signAddress = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_WALLET_SIGN_ADDRESS);

    return dataProvider.signMessage(wallet, signAddress, message);
  }

  /**
   * 
   */
  public Boolean verifyMessage(
      @Nonnull String signature,
      @Nonnull String message) throws DfxException {
    LOGGER.trace("verifyMessage() ...");

    String verifyAddress = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_VERIFY_ADDRESS);

    return dataProvider.verifyMessage(verifyAddress, signature, message);
  }
}
