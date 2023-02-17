package ch.dfx.common.logging;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.EventBus;

/**
 * 
 */
public class MessageEventBus {
  private static final Logger LOGGER = LogManager.getLogger(MessageEventBus.class);

  private static MessageEventBus instance = null;

  private final EventBus eventBus;

  /**
   * 
   */
  public static MessageEventBus getInstance() {
    if (null == instance) {
      instance = new MessageEventBus();
    }

    return instance;
  }

  /**
   * 
   */
  public void register(@Nonnull Object object) {
    LOGGER.trace("register()");
    Objects.requireNonNull(object, "register null object is not allowed");

    LOGGER.trace("register(): " + object.getClass().getSimpleName());
    eventBus.register(object);
  }

  /**
   * 
   */
  public void unregister(@Nonnull Object object) {
    LOGGER.trace("unregister()");
    Objects.requireNonNull(object, "unregister null object is not allowed");

    LOGGER.trace("unregister(): " + object.getClass().getSimpleName());
    eventBus.unregister(object);
  }

  /**
   * 
   */
  public void postEvent(@Nonnull Object event) {
    LOGGER.trace("postEvent()");
    Objects.requireNonNull(event, "null event is not allowed");

    try {
      LOGGER.trace("postEvent(): " + event.getClass().getSimpleName() + ": " + event.toString());
      eventBus.post(event);
    } catch (Throwable t) {
      LOGGER.error("postEvent", t);
    }
  }

  /**
   * 
   */
  private MessageEventBus() {
    this.eventBus = new EventBus();
  }
}
