package ch.dfx.logging;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.transactionserver.scheduler.TimerProviderTask;

/**
 * 
 */
public class MessageSender extends TimerProviderTask {
  private static final Logger LOGGER = LogManager.getLogger(MessageSender.class);

  private final String id;
  private int counter = 0;

  private boolean isProcessing = false;

  /**
   * 
   */
  public MessageSender(@Nonnull String id) {
    this.id = id;
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    isProcessing = true;

    counter++;

    String message =
        new StringBuilder()
            .append("[").append(id).append("] ").append(counter)
            .toString();
    LOGGER.debug(message);

    MessageEventBus.getInstance().postEvent(new TelegramAutomaticInformationBotEvent(message));

    isProcessing = false;
  }
}
