package ch.dfx.logging;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.logging.events.MessageEvent;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class MessageEventProvider implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(MessageEventProvider.class);

  private final MessageEventCollector messageEventCollector;

  private boolean isProcessing = false;

  /**
   * 
   */
  public MessageEventProvider(@Nonnull MessageEventCollector messageEventCollector) {
    this.messageEventCollector = messageEventCollector;
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
      LOGGER.debug("[MESSAGE]");
      LOGGER.debug(messageEvent);
    }
  }
}
