package ch.dfx.common.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.Subscribe;

import ch.dfx.common.logging.events.MessageEvent;

/**
 * 
 */
public class MessageEventCollector {
  private static final Logger LOGGER = LogManager.getLogger(MessageEventCollector.class);

  private final List<MessageEvent> syncMessageList;

  /**
   * 
   */
  public MessageEventCollector() {
    this.syncMessageList = Collections.synchronizedList(new ArrayList<>());
  }

  /**
   * 
   */
  public List<MessageEvent> getMessageEventList() {
    LOGGER.trace("getMessageEventList()");

    List<MessageEvent> copyMessageList = Collections.synchronizedList(new ArrayList<>(syncMessageList));
    syncMessageList.removeAll(copyMessageList);

    return copyMessageList;
  }

  /**
   * 
   */
  @Subscribe
  public void executeEvent(@Nonnull MessageEvent messageEvent) {
    LOGGER.trace("executeEvent()");

    syncMessageList.add(messageEvent);
  }
}
