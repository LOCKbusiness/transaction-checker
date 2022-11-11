package ch.dfx.logging;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.logging.events.MessageEvent;
import ch.dfx.transactionserver.scheduler.TimerProvider;
import ch.dfx.transactionserver.scheduler.TimerProviderTask;

/**
 * 
 */
public class MessageEventProviderMain extends TimerProviderTask {
  private static final Logger LOGGER = LogManager.getLogger(MessageEventProviderMain.class);

  private final MessageEventCollector messageEventCollector;

  private boolean isProcessing = false;

  /**
   * 
   */
  public static void main(String[] args) {

    try {
      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename("test", NetworkEnum.TESTNET));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      LOGGER.debug("start");

      MessageEventProviderMain messageEventProviderTest = new MessageEventProviderMain();
      messageEventProviderTest.setup();
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  public MessageEventProviderMain() {
    messageEventCollector = new MessageEventCollector();
    MessageEventBus.getInstance().register(messageEventCollector);
  }

  /**
   * 
   */
  public void setup() {
    LOGGER.debug("setup()");

    for (int i = 0; i < 10; i++) {
      UUID messageSenderA = TimerProvider.getInstance().add(new MessageSender("A" + i), 0, 1);
      UUID messageSenderB = TimerProvider.getInstance().add(new MessageSender("B" + i), 0, 1);
      UUID messageSenderC = TimerProvider.getInstance().add(new MessageSender("C" + i), 0, 1);

      TimerProvider.getInstance().execute(messageSenderA);
      TimerProvider.getInstance().execute(messageSenderB);
      TimerProvider.getInstance().execute(messageSenderC);
    }

    UUID messageReceiver = TimerProvider.getInstance().add(this, 30, 1);
    TimerProvider.getInstance().execute(messageReceiver);
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run()");

    isProcessing = true;

    List<MessageEvent> messageEventList = messageEventCollector.getMessageEventList();

    for (MessageEvent messageEvent : messageEventList) {
      LOGGER.debug(messageEvent.getMessage());
    }

    isProcessing = false;
  }
}
