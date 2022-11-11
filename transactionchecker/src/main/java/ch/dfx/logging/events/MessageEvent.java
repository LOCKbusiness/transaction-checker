package ch.dfx.logging.events;

import java.util.Objects;

import javax.annotation.Nonnull;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class MessageEvent {

  private final String message;

  /**
   * 
   */
  public MessageEvent(@Nonnull String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }

  @Override
  public int hashCode() {
    return message.hashCode();
  }

  @Override
  public boolean equals(Object otherObject) {
    if (null == otherObject
        || getClass() != otherObject.getClass()) {
      return false;
    }

    if (this == otherObject) {
      return true;
    }

    MessageEvent otherMessageEvent = (MessageEvent) otherObject;
    return Objects.equals(message, otherMessageEvent.message);
  }
}
