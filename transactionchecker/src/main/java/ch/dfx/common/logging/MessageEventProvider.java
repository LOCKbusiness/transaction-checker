package ch.dfx.common.logging;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.logging.events.MessageEvent;
import ch.dfx.common.logging.notifier.TelegramNotifier;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class MessageEventProvider implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(MessageEventProvider.class);

  private final MessageEventCollector messageEventCollector;
  private final TelegramNotifier telegramNotifier;

  private boolean isProcessing = false;

  /**
   * 
   */
  public MessageEventProvider(@Nonnull MessageEventCollector messageEventCollector) {
    this.messageEventCollector = messageEventCollector;

    this.telegramNotifier = new TelegramNotifier();
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run()");

    isProcessing = true;

    try {
      messageProcessing();
    } catch (Throwable t) {
      LOGGER.error("run", t);
    } finally {
      isProcessing = false;
    }
  }

  /**
   * 
   */
  private void messageProcessing() {
    LOGGER.trace("messageProcessing()");

    List<MessageEvent> messageEventList = messageEventCollector.getMessageEventList();

    for (MessageEvent messageEvent : messageEventList) {
      LOGGER.info("[MESSAGE] " + messageEvent);

      String telegramToken = messageEvent.getTelegramToken();
      String telegramChatId = messageEvent.getTelegramChatId();

      if (null != telegramToken
          && null != telegramChatId) {
        telegramNotifier.sendMessage(telegramToken, telegramChatId, messageEvent.getMessage());
      }
    }
  }
}
