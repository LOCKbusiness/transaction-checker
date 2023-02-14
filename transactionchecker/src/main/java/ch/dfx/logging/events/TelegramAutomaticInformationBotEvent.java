package ch.dfx.logging.events;

import javax.annotation.Nonnull;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;

/**
 * 
 */
public class TelegramAutomaticInformationBotEvent extends MessageEvent {

  /**
   * 
   */
  public TelegramAutomaticInformationBotEvent(@Nonnull String message) {
    super(message);

    setTelegramToken(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN));
    setTelegramChatId(ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID));
  }
}
