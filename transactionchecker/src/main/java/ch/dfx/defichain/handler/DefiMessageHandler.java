package ch.dfx.defichain.handler;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class DefiMessageHandler {
  private static final Logger LOGGER = LogManager.getLogger(DefiMessageHandler.class);

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public DefiMessageHandler(@Nonnull DefiDataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  /**
   * 
   */
  public String signMessage(@Nonnull String message) throws DfxException {
    LOGGER.trace("signMessage()");

    String wallet = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_WALLET_NAME);
    String password = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_WALLET_PASSWORD);
    String signAddress = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_WALLET_SIGN_ADDRESS);

    if (null == wallet) {
      throw new DfxException("wallet is null");
    }

    if (null == password) {
      throw new DfxException("password is null");
    }

    if (null == signAddress) {
      throw new DfxException("signAddress is null");
    }

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
    LOGGER.trace("verifyMessage()");
    return dataProvider.verifyMessage(verifyAddress, signature, message);
  }
}
