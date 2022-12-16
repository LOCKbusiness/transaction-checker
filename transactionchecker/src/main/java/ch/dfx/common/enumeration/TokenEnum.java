package ch.dfx.common.enumeration;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 
 */
public enum TokenEnum {
  DFI(0),
  DUSD(15);

  // ...
  private static final Map<Integer, TokenEnum> numberToTokenCache;

  static {
    numberToTokenCache = new HashMap<>();

    for (TokenEnum token : TokenEnum.values()) {
      numberToTokenCache.put(token.getNumber(), token);
    }
  }

  /**
   * 
   */
  public static @Nullable TokenEnum createWithNumber(int number) {
    return numberToTokenCache.get(number);
  }

  /**
   * 
   */
  public static @Nonnull TokenEnum createWithText(
      @Nullable String text,
      @Nonnull TokenEnum defaultToken) {
    try {
      if (null != text) {
        return TokenEnum.valueOf(text.toUpperCase());
      }
    } catch (Throwable t) {
      // Intentionally left blank ...
    }

    return defaultToken;
  }

  // ...
  private final int number;

  /**
   * 
   */
  private TokenEnum(int number) {
    this.number = number;
  }

  /**
   * 
   */
  public int getNumber() {
    return number;
  }
}
