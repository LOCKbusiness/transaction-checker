package ch.dfx.logging.events;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public abstract class MessageEvent {

  private String telegramToken = null;
  private String telegramChatId = null;

  private final String message;

  /**
   * 
   */
  public MessageEvent(@Nonnull String message) {
    this.message = message;
  }

  public @Nullable String getTelegramToken() {
    return telegramToken;
  }

  public void setTelegramToken(@Nullable String telegramToken) {
    this.telegramToken = telegramToken;
  }

  public @Nullable String getTelegramChatId() {
    return telegramChatId;
  }

  public void setTelegramChatId(@Nullable String telegramChatId) {
    this.telegramChatId = telegramChatId;
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

    return Objects.equals(telegramToken, otherMessageEvent.telegramToken)
        && Objects.equals(telegramChatId, otherMessageEvent.telegramChatId)
        && Objects.equals(message, otherMessageEvent.message);
  }
}
