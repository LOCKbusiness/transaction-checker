package ch.dfx.logging.events;

import javax.annotation.Nonnull;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;

/**
 * 
 */
public class TelegramAutomaticVaultInformationBotEvent extends MessageEvent {

  /**
   * 
   */
  public TelegramAutomaticVaultInformationBotEvent(@Nonnull String message) {
    super(message);

    setTelegramToken(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.TELEGRAM_AUTOMATIC_VAULT_INFORMATION_TOKEN));
    setTelegramChatId(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.TELEGRAM_AUTOMATIC_VAULT_INFORMATION_CHAT_ID));
  }
}
