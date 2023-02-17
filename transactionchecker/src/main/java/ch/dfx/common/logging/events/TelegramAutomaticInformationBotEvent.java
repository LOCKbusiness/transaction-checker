package ch.dfx.common.logging.events;

import javax.annotation.Nonnull;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.config.ConfigProvider;

/**
 * 
 */
public class TelegramAutomaticInformationBotEvent extends MessageEvent {

  /**
   * 
   */
  public TelegramAutomaticInformationBotEvent(@Nonnull String message) {
    super(message);

    setTelegramToken(ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN));
    setTelegramChatId(ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID));
  }
}
