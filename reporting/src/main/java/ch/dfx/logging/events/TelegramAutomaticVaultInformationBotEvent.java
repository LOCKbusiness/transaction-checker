package ch.dfx.logging.events;

import javax.annotation.Nonnull;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.logging.events.MessageEvent;
import ch.dfx.config.ReportingConfigEnum;

/**
 * 
 */
public class TelegramAutomaticVaultInformationBotEvent extends MessageEvent {

  /**
   * 
   */
  public TelegramAutomaticVaultInformationBotEvent(@Nonnull String message) {
    super(message);

    setTelegramToken(ConfigProvider.getInstance().getValue(ReportingConfigEnum.TELEGRAM_AUTOMATIC_VAULT_INFORMATION_TOKEN));
    setTelegramChatId(ConfigProvider.getInstance().getValue(ReportingConfigEnum.TELEGRAM_AUTOMATIC_VAULT_INFORMATION_CHAT_ID));
  }
}
